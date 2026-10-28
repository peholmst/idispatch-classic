package net.pkhapps.idispatch.server.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
class WebSocketConfig implements WebSocketConfigurer {

    static final String RUNBOARD_PATH = "/ws/runboard";

    private final RunboardWebSocketHandler runboardWebSocketHandler;

    WebSocketConfig(RunboardWebSocketHandler runboardWebSocketHandler) {
        this.runboardWebSocketHandler = runboardWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Runboards are not browsers and send no Origin header, so any origin is accepted.
        registry.addHandler(runboardWebSocketHandler, RUNBOARD_PATH).setAllowedOriginPatterns("*");
    }
}
