package com.aprendizaje.oauth.services;

import com.aprendizaje.commons.usuarios.models.entities.Usuario;

public interface IUsuarioService {

	Usuario findByUsername(String username);

	Usuario update(Usuario usuario, Long id);

}
