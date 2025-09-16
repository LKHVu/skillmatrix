package com.das.skillmatrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private T data;

    private boolean success;

    private ErrorResponse error;
    
}
