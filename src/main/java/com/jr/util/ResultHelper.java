package com.jr.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
public class ResultHelper<T> {
    private String mess;
    private T t;
    private List<T> list;
    private Boolean bool;
}