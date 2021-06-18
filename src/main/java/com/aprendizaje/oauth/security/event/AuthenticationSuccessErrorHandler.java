package com.aprendizaje.oauth.security.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.aprendizaje.commons.usuarios.models.entities.Usuario;
import com.aprendizaje.oauth.services.IUsuarioService;

import brave.Tracer;
import feign.FeignException;

// Esta clase es para manejar el éxito y error en la autenticación.
// Luego la registraremos en la configuración de Spring security
@Component
public class AuthenticationSuccessErrorHandler implements AuthenticationEventPublisher {

	Logger log = LoggerFactory.getLogger(AuthenticationSuccessErrorHandler.class);

	// Inyectamos para poder cambiar el número de intentos de login
	@Autowired
	IUsuarioService usuarioService;

	// Inyectamos para poder agregar información para Zipkin
	Tracer tracer;

	@Override
	public void publishAuthenticationSuccess(Authentication authentication) {
		UserDetails user = (UserDetails) authentication.getPrincipal();
		String message = "Usuario autenticado: " + user.getUsername();
		System.out.println(message);
		log.info(message);

		String username = authentication.getName();

		var usuario = usuarioService.findByUsername(username);

		if (usuario.getIntentos() != null && usuario.getIntentos() != 0) {
			usuario.setIntentos(0);
			usuarioService.update(usuario, usuario.getId());
		}
	}

	@Override
	public void publishAuthenticationFailure(AuthenticationException exception,
			Authentication authentication) {
		String message = "Error de autenticación: " + exception.getMessage();
		log.error(message);
		System.out.println(message);


		String username = authentication != null? authentication.getName() : null;

		try {
			var errorsForZipkin = new StringBuilder();
			errorsForZipkin.append(message);


			Usuario usuario = usuarioService.findByUsername(username);


			if (usuario.getIntentos() == null) {
				usuario.setIntentos(0);
			}

			log.info("Intentos actual es de: " + usuario.getIntentos());

			usuario.setIntentos(usuario.getIntentos() + 1);

			log.info("Intentos después es de: " + usuario.getIntentos());

			errorsForZipkin.append(" - Intentos de login: " + usuario.getIntentos());

			if (usuario.getIntentos() >= 3) {
				log.error(String.format("El usuario %s se ha deshabilitado.", username));
				errorsForZipkin.append(" - El usuario " + username+ " se ha dehabilitado");
				usuario.setEnabled(false);
			}

			usuarioService.update(usuario, usuario.getId());

			tracer.currentSpan().tag("errors.message", errorsForZipkin.toString());
		} catch (FeignException e) {
			log.error(String.format("El usuario %s no existe", username));
		}
	}

}
