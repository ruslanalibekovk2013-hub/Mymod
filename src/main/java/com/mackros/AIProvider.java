package com.mackros;

import java.util.concurrent.CompletableFuture;

/** Провайдер понимания запроса: превращает текст игрока в NeroSpec. Команды Minecraft провайдер не возвращает. */
public interface AIProvider {
    String name();

    CompletableFuture<NeroResponse> process(NeroRequest request);
}
