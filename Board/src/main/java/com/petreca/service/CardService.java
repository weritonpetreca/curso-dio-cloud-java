package com.petreca.service;

import com.petreca.dto.BoardColumnInfoDTO;
import com.petreca.exception.CardBlockedException;
import com.petreca.exception.CardFinishedException;
import com.petreca.exception.EntityNotFoundException;
import com.petreca.persistence.dao.BlockDAO;
import com.petreca.persistence.dao.CardDAO;
import com.petreca.persistence.entity.CardEntity;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static com.petreca.persistence.entity.BoardColumnKindEnum.CANCEL;
import static com.petreca.persistence.entity.BoardColumnKindEnum.FINAL;

@AllArgsConstructor
public class CardService {

    private final Connection connection;

    public CardEntity insert(final CardEntity entity) throws SQLException {
        try{
            var dao = new CardDAO(connection);
            dao.insert(entity);
            connection.commit();
            return entity;
        }catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }

    public void moveToNextColumn(final Long cardId, final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
        try{
            var dao = new CardDAO(connection);
            var optional = dao.findById(cardId);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O Card de Id %s não foi encontrado".formatted(cardId)));
            if (dto.blocked()){
                throw new CardBlockedException(("O Card de Id %s está bloqueado.\n" +
                                                "É necessário desbloquea-lo primeiro para movê-lo").formatted(cardId));
            }
            var currentColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("O Card informado pertence a outro Board"));
            if (currentColumn.kind().equals(FINAL)){
                throw new CardFinishedException("O Card de Id %s já foi finalizado".formatted(cardId));
            }
            var nextColumn= boardColumnsInfo.stream().filter(bc -> bc.order() == currentColumn.order() + 1)
                    .findFirst().orElseThrow(() -> new IllegalStateException("O Card está cancelado."));
            dao.moveToColumn(nextColumn.id(), cardId);
            System.out.println("Card '%s' movido para a Coluna '%s'".formatted(dto.title(), nextColumn.name()));
            connection.commit();
        }catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }

    public void cancel(final Long cardId, final Long cancelColumnId,
                       final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
        try {
            var dao = new CardDAO(connection);
            var optional = dao.findById(cardId);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O Card de Id %s não foi encontrado".formatted(cardId)));
            if (dto.blocked()) {
                throw new CardBlockedException(("O Card de Id %s está bloqueado.\n" +
                                                "É necessário desbloquea-lo primeiro para movê-lo").formatted(cardId));
            }
            var currentColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("O Card informado pertence a outro Board"));
            if (currentColumn.kind().equals(FINAL)){
                throw new CardFinishedException("O Card de Id %s já foi finalizado".formatted(cardId));
            }
            var cancelColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(cancelColumnId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Coluna de cancelamento não encontrada"));
            boardColumnsInfo.stream().filter(bc -> bc.order() == currentColumn.order() + 1)
                    .findFirst().orElseThrow(() -> new IllegalStateException("O Card está cancelado."));
            dao.moveToColumn(cancelColumnId, cardId);
            System.out.printf("Card '%s' movido para a Coluna '%s'%n", dto.title(), cancelColumn.name());
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }

    public void block(final Long id, final String reason, final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
        try {
            var dao = new CardDAO(connection);
            var optional = dao.findById(id);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O Card de Id %s não foi encontrado".formatted(id)));
            if (dto.blocked()) {
                throw new CardBlockedException(("O Card de Id %s já está bloqueado").formatted(id));
            }
            var currentColumn = boardColumnsInfo.stream().filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst().orElseThrow();
            if (currentColumn.kind().equals(FINAL) || currentColumn.kind().equals(CANCEL)){
                var message = "O Card já está em uma coluna do tipo %s e não pode ser bloqueado"
                        .formatted(currentColumn.kind());
                throw new IllegalStateException(message);
            }
            var blockDAO = new BlockDAO(connection);
            blockDAO.block(reason, id);
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }

    public void unblock(final Long id, final String reason) throws SQLException{
        try{
            var dao = new CardDAO(connection);
            var optional = dao.findById(id);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O Card de Id %s não foi encontrado".formatted(id)));
            if (!dto.blocked()) {
                throw new CardBlockedException(("O Card de Id %s não está bloqueado").formatted(id));
            }
            var blockDAO = new BlockDAO(connection);
            blockDAO.unblock(reason, id);
            connection.commit();
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }
}
