package com.petreca.ui;

import com.petreca.persistence.config.ConnectionConfig;
import com.petreca.persistence.entity.BoardColumnEntity;
import com.petreca.persistence.entity.BoardColumnKindEnum;
import com.petreca.persistence.entity.BoardEntity;
import com.petreca.service.BoardQueryService;
import com.petreca.service.BoardService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.petreca.persistence.config.ConnectionConfig.getConnection;
import static com.petreca.persistence.entity.BoardColumnKindEnum.*;

public class MainMenu {

    private static final Scanner scanner = new Scanner(System.in).useDelimiter("\n");

    public void execute() throws SQLException {
        System.out.println("Bem vindo ao gerenciador de Boards.\nEscolha a opção desejada:");
        while (true) {
            System.out.println("1 - Criar novo Board.");
            System.out.println("2 - Selecionar um Board existente.");
            System.out.println("3 - Excluir um Board existente.");
            System.out.println("4 - Sair.");
        
        int option = scanner.nextInt();
        
        switch (option) {
            case 1 -> createBoard();
            case 2 -> selectBoard();
            case 3 -> deleteBoard();
            case 4 -> System.exit(0);
            default -> System.out.println("Opção inválida. Informe uma opção do Menu.");
        }
        }
    }

    private void createBoard() throws SQLException {
        var entity = new BoardEntity();
        System.out.println("Informe o nome do Board a ser criado:");
        entity.setName(scanner.next());

        System.out.println("Seu Board terá mais de 3 colunas? Se sim, informe quantas, caso não, digite '0'");
        var additionalColumns = scanner.nextInt();

        List<BoardColumnEntity> columns = new ArrayList<>();

        System.out.println("Informe o nome da coluna inicial do Board:");
        var initialColumnName = scanner.next();
        var initialColumn = createColumn(initialColumnName, INITIAL, 0);
        columns.add(initialColumn);

        for (int i = 0; i < additionalColumns; i++) {
            System.out.println("Informe o nome da coluna de tarefa pendente do Board:");
            var pendingColumnName = scanner.next();
            var pendingColumn = createColumn(pendingColumnName, PENDING, i + 1);
            columns.add(pendingColumn);
        }

        System.out.println("Informe o nome da coluna final do Board:");
        var finalColumnName = scanner.next();
        var finalColumn = createColumn(finalColumnName, FINAL, additionalColumns+1);
        columns.add(finalColumn);

        System.out.println("Informe o nome da coluna de cancelamento do Board:");
        var cancelColumnName = scanner.next();
        var cancelColumn = createColumn(cancelColumnName, CANCEL, additionalColumns+2);
        columns.add(cancelColumn);

        entity.setBoardColumns(columns);

        Connection connection = null;

        try{
            connection = getConnection();
            var service = new BoardService(connection);
            service.insert(entity);
            connection.commit();
            System.out.printf("O Board %s foi criado com sucesso.\n", entity.getName());
        } catch (SQLException e) {
            if (connection != null) {
                connection.rollback();
            }
            System.out.println("Erro ao inserir o Board no banco de dados: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

    }

    private void selectBoard() throws SQLException {
        System.out.println("Informe o ID do Board a ser selecionado:");
        var id = scanner.nextLong();
        try(var connection = getConnection()){
            var queryService = new BoardQueryService(connection);
            var optional = queryService.findById(id);
            optional.ifPresentOrElse(
                    b -> new BoardMenu(b).execute(),
                    () -> System.out.printf("Não foi encontrado um Board com o id %s.\n", id)
            );
        }
    }

    private void deleteBoard() throws SQLException {
        System.out.println("Informe o ID do Board a ser excluído:");
        var id = scanner.nextLong();
        try(var connection = getConnection()){
            var service = new BoardService(connection);
            if (service.delete(id)){
                System.out.printf("O Board %s foi excluído com sucesso.\n", id);
            }
                System.out.printf("Não foi encontrado com o id %s\n", id);
        }
    }

    private BoardColumnEntity createColumn(final String name, final BoardColumnKindEnum kind, final int order){
        var boardColumn = new BoardColumnEntity();
        boardColumn.setName(name);
        boardColumn.setOrder(order);
        boardColumn.setKind(kind);
        return boardColumn;
    }
}