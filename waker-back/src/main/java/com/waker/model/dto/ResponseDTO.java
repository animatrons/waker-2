package com.waker.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResponseDTO<T> {

    private T data;
    private String status;
    // TODO: generate messages by data class name, key (&/or name), and method, AND status (success or fail)
    private String message;
    private Integer page;
    private Integer pageSize;
    private Integer start;
    private Integer total;

    public ResponseDTO(T obj, String status, String message) {
        this.data = obj;
        this.status = status;
        this.message = message;
        this.page = null;
        this.start = null;
        this.pageSize = null;
        this.total = null;
    }
}
