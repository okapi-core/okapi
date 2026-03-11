package org.okapi.oscar.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "okapi.oscar.memory", name = "enabled", havingValue = "true")
public class JdbcChatMemory {
  @Bean
  public ChatMemoryRepository chatMemoryRepository(
      @Autowired JdbcChatMemoryRepository chatMemoryRepository) {
    return chatMemoryRepository;
  }

  @Bean
  public ChatMemory chatMemory(ChatMemoryRepository repository) {
    return MessageWindowChatMemory.builder()
            .chatMemoryRepository(repository)
            .build();
  }
}
