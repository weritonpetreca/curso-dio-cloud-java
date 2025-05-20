package com.petreca.ui;

import com.petreca.dto.BoardColumnInfoDTO;
import com.petreca.persistence.entity.BoardColumnEntity;
import com.petreca.persistence.entity.BoardEntity;
import com.petreca.persistence.entity.CardEntity;
import com.petreca.service.BoardColumnQueryService;
import com.petreca.service.BoardQueryService;
import com.petreca.service.CardQueryService;
import com.petreca.service.CardService;
import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.Scanner;

import static com.petreca.persistence.config.ConnectionConfig.getConnection;

@AllArgsConstructor
public class BoardMenu {

    private final Scanner scanner = new Scanner(System.in).useDelimiter("\n");

    private final BoardEntity entity;

    public void execute() {
        try {
            System.out.println("Bem vindo ao Board " + entity.getName() + ".\nEscolha a opção desejada:\n");
            var option = -1;
            while (option != 9) {
                System.out.println("1 - Criar um novo Card.");
                System.out.println("2 - Mover um Card.");
                System.out.println("3 - Bloquear um Card.");
                System.out.println("4 - Desbloquear um Card.");
                System.out.println("5 - Cancelar um Card.");
                System.out.println("6 - Visualizar o Board com as colunas e Cards.");
                System.out.println("7 - Visualizar colunas com Cards.");
                System.out.println("8 - Visualizar Card.");
                System.out.println("9 - Voltar para o Menu Principal.");
                System.out.println("10 - Sair.");

                option = scanner.nextInt();

                switch (option) {
                    case 1 -> createCard();
                    case 2 -> moveCardToNextColumn();
                    case 3 -> blockCard();
                    case 4 -> unblockCard();
                    case 5 -> cancelCard();
                    case 6 -> showBoard();
                    case 7 -> showColumn();
                    case 8 -> showCard();
                    case 9 -> System.out.println("Voltando ao Menu Principal...");
                    case 10 -> System.exit(0);
                    default -> System.out.println("Opção inválida. Informe uma opção do Menu.");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private void createCard() throws SQLException {
        var card = new CardEntity();
        System.out.println("Informe o título do Card:");
        card.setTitle(scanner.next());
        System.out.println("Informe a descrição do Card:");
        card.setDescription(scanner.next());
        card.setBoardColumn(entity.getInitialColumn());
        try (var connection = getConnection()) {
            new CardService(connection).insert(card);
            System.out.printf("O Card [%s] foi criado com sucesso.\n", card.getTitle());
        }
    }

    private void moveCardToNextColumn() throws SQLException {
        System.out.println("Informe o ID do Card a ser movido:");
        var cardId = scanner.nextLong();
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind(), bc.getName()))
                .toList();
        try (var connection = getConnection()) {
            new CardService(connection).moveToNextColumn(cardId, boardColumnsInfo);
        } catch (RuntimeException ex) {
            System.out.println("Erro ao mover o Card: " + ex.getMessage());
        }
    }

    private void blockCard() throws SQLException {
        System.out.println("Informe o ID do Card a ser bloqueado:");
        var cardId = scanner.nextLong();
        System.out.println("Informe o motivo do bloqueio do Card:");
        var blockReason = scanner.next();
        var boardColumnsInfo = entity.getBoardColumns().stream().map(bc ->
                new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind(), bc.getName())).toList();
        try (var connection = getConnection()) {
            var cardQuery = new CardQueryService(connection);
            var card = cardQuery.findById(cardId).orElseThrow();
            new CardService(connection).block(cardId, blockReason, boardColumnsInfo);
            System.out.printf("O Card '%s' foi bloqueado com sucesso.%n", card.title());
        } catch (RuntimeException ex) {
            System.out.println("Erro ao bloquear o Card: " + ex.getMessage());
        }
    }

    private void unblockCard() throws SQLException {
        System.out.println("Informe o ID do Card a ser desbloqueado:");
        var cardId = scanner.nextLong();
        System.out.println("Informe o motivo do desbloqueio do Card:");
        var unblockReason = scanner.next();
        try (var connection = getConnection()) {
            var cardQuery = new CardQueryService(connection);
            var card = cardQuery.findById(cardId).orElseThrow();
            new CardService(connection).unblock(cardId, unblockReason);
            System.out.printf("O Card '%s' foi desbloqueado com sucesso.%n", card.title());
        } catch (RuntimeException ex) {
            System.out.println("Erro ao desbloquear o Card: " + ex.getMessage());
        }
    }

    private void cancelCard() throws SQLException {
        System.out.println("Informe o ID do Card a ser cancelado:");
        var cardId = scanner.nextLong();
        var cancelColumn = entity.getCancelColumn(cardId);
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind(), bc.getName()))
                .toList();
        try (var connection = getConnection()) {
            new CardService(connection).cancel(cardId, cancelColumn.getId(), boardColumnsInfo);
        } catch (RuntimeException ex) {
            System.out.println("Erro ao cancelar o Card: " + ex.getMessage());
        }
    }

    private void showBoard() throws SQLException {
        try (var connection = getConnection()) {
            var optional = new BoardQueryService(connection).showBoardDeatails(entity.getId());
            optional.ifPresent(b -> {
                System.out.printf("Board [%s,%s]\n", b.id(), b.name());
                b.columns().forEach(c ->
                        System.out.printf("Coluna [%s] tipo: [%s] tem %s Cards\n", c.name(), c.kind(), c.cardsAmount())
                );
            });
        }
    }

    private void showColumn() throws SQLException {
        var columnsIds = entity.getBoardColumns().stream().map(BoardColumnEntity::getId).toList();
        var selectedColum = -1L;
        while (!columnsIds.contains(selectedColum)) {
            System.out.printf("Escolha uma coluna do Board %s\n", entity.getName());
            entity.getBoardColumns().forEach(c -> System.out.printf("%s - %s [%s]\n", c.getId(), c.getName(), c.getKind()));
            selectedColum = scanner.nextLong();
        }
        try (var connection = getConnection()) {
            var column = new BoardColumnQueryService(connection).findById(selectedColum);
            column.ifPresent(co -> {
                System.out.printf("Coluna [%s] tipo: [%s]\n", co.getName(), co.getKind());
                co.getCards().forEach(ca ->
                        System.out.printf("Card [%s] - [%s].\nDescrição: %s\n", ca.getId(), ca.getTitle(), ca.getDescription()));
            });
        }
    }

    private void showCard() throws SQLException {
        System.out.println("Informe o ID do Card a ser visualizado:");
        var selectedCardId = scanner.nextLong();
        try (var connection = getConnection()) {
            new CardQueryService(connection).findById(selectedCardId).ifPresentOrElse(
                    c -> {
                        System.out.printf("Card [%s] - [%s].\nDescrição: %s\n", c.id(), c.title(), c.description());
                        System.out.println(c.blocked() ? "Card bloqueado. Motivo: " + c.blockReason() : "Card não bloqueado.");
                        System.out.printf("Já foi bloqueado %s vezes.\n", c.blocksAmount());
                        System.out.printf("Está no momento na coluna [%s] - [%s].\n", c.columnId(), c.columnName());
                    },
                    () -> System.out.printf("Não foi encontrado um Card com o id %s.\n", selectedCardId)
            );
        }
    }
}