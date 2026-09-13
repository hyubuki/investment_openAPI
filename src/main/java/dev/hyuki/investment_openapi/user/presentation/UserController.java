package dev.hyuki.investment_openapi.user.presentation;

import dev.hyuki.investment_openapi.user.dto.UserRegistrationRequest;
import dev.hyuki.investment_openapi.user.dto.UserRegistrationResponse;
import dev.hyuki.investment_openapi.user.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public ResponseEntity<UserRegistrationResponse> register(
      @Valid @RequestBody UserRegistrationRequest request
  ) {
    UserRegistrationResponse response = UserRegistrationResponse.from(
        userService.register(request.email(), request.password())
    );
    URI location = URI.create("/api/v1/users/" + response.userId());
    return ResponseEntity.created(location).body(response);
  }
}
