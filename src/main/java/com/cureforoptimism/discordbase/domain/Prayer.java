package com.cureforoptimism.discordbase.domain;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Prayer {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Getter Long discordUserId;

  @Getter String discordId;

  @Getter Date createdAt;
}