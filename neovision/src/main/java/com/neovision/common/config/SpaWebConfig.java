package com.neovision.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve el frontend React empaquetado dentro del mismo backend (para el
 * despliegue de demo en un solo App Service). React Router maneja estas
 * rutas del lado del cliente, pero si alguien las carga directo o recarga
 * la pagina, el servidor tiene que devolver index.html para que React
 * Router tome el control — sin esto, esas rutas dan 404.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/registro").setViewName("forward:/index.html");
        registry.addViewController("/dashboard").setViewName("forward:/index.html");
        registry.addViewController("/admin").setViewName("forward:/index.html");
        registry.addViewController("/estudiante").setViewName("forward:/index.html");
        registry.addViewController("/docente").setViewName("forward:/index.html");
    }
}
