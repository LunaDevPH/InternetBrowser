package com.android5.internetbrowser;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface OpenRouterService {
    
    class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    class Request {
        public String model;
        public List<Message> messages;

        public Request(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }
    }

    class Response {
        public List<Choice> choices;

        public static class Choice {
            public Message message;
        }
    }

    class ErrorResponse {
        public Error error;

        public static class Error {
            public String message;
            public int code;
        }
    }

    @POST("api/v1/chat/completions")
    @Headers({
        "Content-Type: application/json",
        "HTTP-Referer: https://internetbrowser.app",
        "X-Title: Internet Browser"
    })
    Call<Response> getCompletion(
        @Header("Authorization") String auth,
        @Body Request request
    );
}