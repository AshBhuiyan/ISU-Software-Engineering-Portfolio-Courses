package onetoone.Users;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.config.GameConfig;
import onetoone.util.ApiError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    UserRepository userRepository;
    
    @Autowired
    ResourceRepository resourceRepository;
    
    @Autowired
    GameConfig gameConfig;

    private String success = "{\"message\":\"success\"}";
    private String failure = "{\"message\":\"failure\"}";

    /**
     * Login endpoint - verifies email and password
     */
    @PostMapping("/users/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            if (request.emailId == null || request.emailId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "Email is required"));
            }
            if (request.password == null || request.password.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "Password is required"));
            }
            
            String email = request.emailId.trim();
            User user = userRepository.findByEmailId(email);
            
            if (user == null) {
                // Try to find all users for debugging (remove in production)
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.USER_NOT_FOUND, "User not found with email: " + email));
            }
            
            if (!user.getPassword().equals(request.password)) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.UNAUTHORIZED, "Invalid password"));
            }
            
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new ApiError(ApiError.BAD_REQUEST, "Login error: " + e.getMessage()));
        }
    }
    @Operation(
            summary = "Get user by email",
            description = "Fetch a user record from the database using their email ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found and returned"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/users")
    public ResponseEntity<?> getByEmail(@RequestParam(required = false) String emailId) {
        try {
            // Handle missing or empty email parameter
            if (emailId == null || emailId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "Email parameter is required. Received: '" + emailId + "'"));
            }
            
            // Trim and decode the email (in case of URL encoding issues)
            String email = emailId.trim();
            
            // Try to URL decode in case of encoding issues
            try {
                email = java.net.URLDecoder.decode(email, "UTF-8");
            } catch (Exception e) {
                // If decoding fails, use original
                System.out.println("URL decode failed, using original: " + email);
            }
            
            // Log for debugging
            System.out.println("GET /users - Searching for email: '" + email + "' (length: " + email.length() + ")");
            
            // Try exact match first
            User user = userRepository.findByEmailId(email);
            
            // If not found, try case-insensitive search
            if (user == null) {
                user = userRepository.findByEmailIdIgnoreCase(email);
            }
            
            if (user == null) {
                // Log all users for debugging
                System.out.println("User not found. Available users in database:");
                java.util.List<User> allUsers = userRepository.findAll();
                if (allUsers.isEmpty()) {
                    System.out.println("  (No users in database)");
                } else {
                    allUsers.forEach(u -> 
                        System.out.println("  - ID: " + u.getId() + ", Email: '" + u.getEmailId() + "', Name: " + u.getName())
                    );
                }
                
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.USER_NOT_FOUND, "User not found with email: " + email));
            }
            
            System.out.println("Found user: ID=" + user.getId() + ", Email='" + user.getEmailId() + "', Name=" + user.getName());
            
            // Validate user data before creating DTO
            if (user.getId() <= 0) {
                System.err.println("ERROR: User has invalid ID: " + user.getId());
                return ResponseEntity.status(500)
                        .body(new ApiError(ApiError.BAD_REQUEST, "User data is invalid"));
            }
            
            // Get password and ensure it's not null
            String userPassword = user.getPassword();
            if (userPassword == null) {
                System.err.println("WARNING: User password is null for user ID: " + user.getId());
                userPassword = "";
            } else {
                // Log password length for debugging (not the actual password)
                System.out.println("User password retrieved - length: " + userPassword.length() + ", isEmpty: " + userPassword.isEmpty());
            }
            
            // Create a safe DTO to avoid lazy loading issues
            UserDTO userDTO = new UserDTO(
                user.getId(),
                user.getName() != null ? user.getName() : "Unknown",
                user.getEmailId() != null ? user.getEmailId() : email,
                userPassword // Frontend needs this for password check
            );
            // Don't include Resource, Role, Avatar to avoid lazy loading exceptions
            
            // Log the DTO for debugging (but don't log actual password in production)
            System.out.println("Returning UserDTO: id=" + userDTO.id + ", name=" + userDTO.name + ", emailId=" + userDTO.emailId + ", passwordLength=" + (userDTO.password != null ? userDTO.password.length() : 0));
            
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in getByEmail: " + e.getClass().getName() + " - " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(new ApiError(ApiError.BAD_REQUEST, "Error retrieving user: " + e.getMessage()));
        }
    }
    
    /**
     * Debug endpoint to list all users (remove in production)
     */
    @GetMapping("/users/all")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
    @Operation(
            summary = "Get user by ID",
            description = "Fetch a user record using their unique ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found and returned"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
@GetMapping("/user/id")
public User getById(@RequestParam Integer id) {
    return userRepository.findById(id).orElse(null);
}
    @Operation(
            summary = "Get user by username",
            description = "Fetch a user record using their username."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found and returned"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
@GetMapping("user/username")
public User getByUsername(@RequestParam String name) {
    User user = userRepository.getByName(name);
    return user;
}

    @Operation(
            summary = "Create a new user",
            description = "Create a new user record in the database."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
@PostMapping("/users")
    @Transactional
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            // Validate input
            if (user.getEmailId() == null || user.getEmailId().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "Email is required"));
            }
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "Password is required"));
            }
            if (user.getName() == null || user.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "Name is required"));
            }
            
            // Trim email and password to avoid whitespace issues
            String email = user.getEmailId().trim();
            String password = user.getPassword().trim();
            user.setEmailId(email);
            user.setPassword(password); // Ensure password is trimmed before saving
            
            // Check if user already exists
            User existingUser = userRepository.findByEmailId(email);
            if (existingUser != null) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "User with email " + email + " already exists"));
            }
            
            // Save user first (without resource initially)
            User savedUser = userRepository.saveAndFlush(user);
            
            // Create Resource entity for the user with default values
            // If this fails, user is still saved and Resource can be created later
            try {
                Resource resource = new Resource(
                    gameConfig.getMaxTurnsPerMonth(),  // turnsLeft
                    0.0,                                // money
                    gameConfig.getBaseCreditScore(),    // credit score
                    gameConfig.getDefaultCreditLimit(),  // credit limit
                    1                                   // currentMonth
                );
                resource.setUser(savedUser);
                Resource savedResource = resourceRepository.saveAndFlush(resource);
                
                // Link resource back to user
                savedUser.setResource(savedResource);
                savedUser = userRepository.saveAndFlush(savedUser);
            } catch (Exception resourceError) {
                // Log but don't fail user creation if Resource creation fails
                System.err.println("Warning: Failed to create Resource for user " + email + ": " + resourceError.getMessage());
                resourceError.printStackTrace();
                // User is still saved, just without Resource (can be created later)
            }
            
            // Verify the user was saved correctly by fetching fresh from DB
            userRepository.flush(); // Ensure all changes are committed
            User verifyUser = userRepository.findByEmailId(email);
            if (verifyUser == null) {
                return ResponseEntity.status(500)
                        .body(new ApiError(ApiError.BAD_REQUEST, "User creation failed - user not found after save"));
            }
            
            return ResponseEntity.ok(verifyUser);
        } catch (Exception e) {
            // Log the exception for debugging
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new ApiError(ApiError.BAD_REQUEST, "Error creating user: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Change a user's password",
            description = "Update the password for an existing user after verifying the old password."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password successfully updated"),
            @ApiResponse(responseCode = "400", description = "Old password did not match")
    })
@PutMapping("/users")
    public String changePassword(@RequestBody User user, @RequestParam String oldPassword, @RequestParam String newPassword) {
    if (user.getPassword().equals(oldPassword)){
        user.setPassword(newPassword);
        userRepository.save(user);
        return success;
    }else{
        return failure;
    }
}
    @Operation(
            summary = "Delete a user by ID",
            description = "Deletes a user record from the database using their ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User successfully deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
@DeleteMapping("/users")
    public String deleteById(@RequestParam int id) {
    User user = userRepository.findById(id).orElse(null);
    if (user != null) {
        userRepository.delete(user);
        return success;
    }else{
        return failure;
    }
    }
    
    /**
     * Login request DTO
     */
    public static class LoginRequest {
        public String emailId;
        public String password;
    }
    
    /**
     * User DTO to avoid lazy loading issues during JSON serialization
     */
    public static class UserDTO {
        @JsonProperty("id")
        public int id;
        
        @JsonProperty("name")
        public String name;
        
        @JsonProperty("emailId")
        public String emailId;
        
        @JsonProperty("password")
        public String password;
        
        // Default constructor for Jackson
        public UserDTO() {}
        
        // Constructor for convenience
        public UserDTO(int id, String name, String emailId, String password) {
            this.id = id;
            this.name = name;
            this.emailId = emailId;
            this.password = password;
        }
    }

}
