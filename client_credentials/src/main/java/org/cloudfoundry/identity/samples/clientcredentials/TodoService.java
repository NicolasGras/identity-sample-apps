package org.cloudfoundry.identity.samples.clientcredentials;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Component
public class TodoService {
    private OAuth2RestTemplate restTemplate;
    private String resourceServer;

    @Autowired
    public TodoService(@Qualifier("clientCredentialsRestTemplate") OAuth2RestTemplate restTemplate, @Value("${resourceServerUrl}") String resourceServer) {
        this.restTemplate = restTemplate;
        this.resourceServer = resourceServer;
    }

    public List<Todo> getAll() {
        ResponseEntity<List<Todo>> response = restTemplate.exchange("{resourceServer}/todo",
            GET, null, new ParameterizedTypeReference<List<Todo>>() {}, resourceServer);
        return response.getBody();
    }

    public Todo create(Todo todo) {
        todo.setTodo("Sent from Client ID " + restTemplate.getResource().getClientId() + ": " + todo.getTodo());
        HttpEntity<Todo> entity = new HttpEntity<>(todo);
        ResponseEntity<Todo> response = restTemplate.exchange("{resourceServer}/todo",
            POST, entity, Todo.class, resourceServer);
        return response.getBody();
    }

    public void delete(String id) {
        restTemplate.exchange("{resourceServer}/todo/{id}", DELETE, null, Void.class, resourceServer, id);
    }

}

class Todo {
    private String id;
    private String todo;
    private Date created;
    private Date updated;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTodo() {
        return todo;
    }

    public void setTodo(String todo) {
        this.todo = todo;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}
