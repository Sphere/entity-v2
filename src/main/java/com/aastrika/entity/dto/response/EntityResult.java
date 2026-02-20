package com.aastrika.entity.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityResult<T> {

  private int count;
  private List<T> entity;

  public static <T> EntityResult<T> of(List<T> items) {
    List<T> list = items != null ? items : List.of();
    return new EntityResult<>(list.size(), list);
  }

  public static <T> EntityResult<T> of(T item) {
    if (item == null) return empty();
    return new EntityResult<>(1, List.of(item));
  }

  public static <T> EntityResult<T> empty() {
    return new EntityResult<>(0, List.of());
  }
}