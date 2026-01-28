package com.wxrley.model;

import java.io.Serializable;

/**
 * Representa um usuário do chat.
 * Record simples contendo apenas o nome do usuário.
 * Utilizado por Message para identificar remetente e por Connection para autenticação.
 */
public record User(String name) implements Serializable {

    private static final long serialVersionUID = 1L;

}