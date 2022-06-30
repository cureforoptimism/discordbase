package com.cureforoptimism.discordbase.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trait {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Getter
  @JsonProperty("trait_type")
  String traitType;

  @Getter String value;

  @ManyToOne
  @Setter
  @JoinColumn(name = "smolage_id")
  SmolAge smolAge;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    Trait trait = (Trait) o;
    return id != null && Objects.equals(id, trait.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
