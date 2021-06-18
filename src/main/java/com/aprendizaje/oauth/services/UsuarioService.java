package com.aprendizaje.oauth.services;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.aprendizaje.commons.usuarios.models.entities.Usuario;
import com.aprendizaje.oauth.clients.UsuarioFeignClient;

import brave.Tracer;
import feign.FeignException;

// Implementamos una interfaz propia de Spring security
@Service
public class UsuarioService implements IUsuarioService, UserDetailsService {

	Logger log = LoggerFactory.getLogger(UsuarioService.class);

	@Autowired
	UsuarioFeignClient client;

	// Inyectamos Tracer para agregar información para Zipkin
	@Autowired
	Tracer tracer;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		try {

			var usuario = client.findByUsername(username);

			List<GrantedAuthority> authorities = usuario.getRoles().stream()
					.map(role -> new SimpleGrantedAuthority(role.getNombre()))
					.collect(Collectors.toList());

			return new User(username, usuario.getPassword(), usuario.getEnabled(), true, true, true,
					authorities);
		} catch (FeignException e) {
			String error = "No se encuentra usuario en base de datos";
			log.error(error);
			tracer.currentSpan().tag("error.message", error + ": " + e.getMessage());
			throw new UsernameNotFoundException("No se encuentra al usuario");
		}
	}

	@Override
	public Usuario findByUsername(String username) {
		return client.findByUsername(username);
	}

	@Override
	public Usuario update(Usuario usuario, Long id) {
		return client.update(usuario, id);
	}

}
