package com.modernization.slice.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Replaces the legacy "write trade file to disk" side effect: same artefact, now driven by an event. */
@Component
public class FileDropAdapter implements OutboundAdapter {
    private final ObjectMapper mapper;
    private final Path dir;

    public FileDropAdapter(ObjectMapper mapper, @Value("${app.adapters.file-dir:./out}") String dir) {
        this.mapper = mapper;
        this.dir = Path.of(dir);
    }

    @Override
    public String name() {
        return "file-feed";
    }

    @Override
    public void deliver(TradePostedEvent e) {
        try {
            Files.createDirectories(dir);
            String line = mapper.writeValueAsString(e) + System.lineSeparator();
            Files.writeString(dir.resolve(e.tenantId() + "-trades.jsonl"), line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
