package onetoone.Roles;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class RoleController {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;
    @Operation(
            summary = "Get role for a user",
            description = "Fetches the Role assigned to a specific user by their userId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @GetMapping("/role/{userId}")
    public ResponseEntity<?> getRoleByUser(@PathVariable int userId) {
        try {
            if (userId <= 0) {
                return ResponseEntity.badRequest().body("Invalid user ID");
            }
            Role role = roleRepository.findByUserId(userId);
            if (role == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error retrieving role: " + e.getMessage());
        }
    }
    @Operation(
            summary = "Assign a role to a user",
            description = "Creates or updates a role for a given user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role assigned or updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/role/{userId}/{roleName}")
    public ResponseEntity<?> assignRole(@PathVariable int userId, @PathVariable String roleName) {
        try {
            if (userId <= 0) {
                return ResponseEntity.badRequest().body("Invalid user ID");
            }
            if (roleName == null || roleName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Role name is required");
            }
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found with ID: " + userId);
            }
            
            Role existing = roleRepository.findByUser(user);
            int accessLevel = getAccessLevel(roleName);
            
            if (existing == null) {
                Role newRole = new Role(roleName, accessLevel);
                user.setRole(newRole);
                newRole.setUser(user);
                roleRepository.save(newRole);
                userRepository.save(user);
                return ResponseEntity.ok("Role assigned: " + roleName);
            } else {
                existing.setRoleName(roleName);
                existing.setAccessLevel(accessLevel);
                user.setRole(existing);
                existing.setUser(user);
                roleRepository.save(existing);
                userRepository.save(user);
                return ResponseEntity.ok("Role updated: " + roleName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error assigning role: " + e.getMessage());
        }
    }
    @Operation(
            summary = "Update an existing role",
            description = "Updates the roleName (and accessLevel) of a Role by its ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @PutMapping("/role/{id}/{roleName}")
    public String updateRole(@PathVariable int id, @PathVariable String roleName) {
        Role role = roleRepository.findById(id);
        if (role == null) return "Role not found";

        role.setRoleName(roleName);
        role.setAccessLevel(getAccessLevel(roleName));
        roleRepository.save(role);

        return "Role updated successfully";
    }
    @Operation(
            summary = "Delete a role",
            description = "Deletes a Role by its ID and unlinks it from any user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @DeleteMapping("/role/{id}")
    public String deleteRole(@PathVariable int id) {
        Role role = roleRepository.findById(id);
        if (role == null) return "Role not found";

        // unlink role from user to avoid FK constraint violation
        User user = role.getUser();
        if (user != null) {
            user.setRole(null);
            userRepository.save(user);
        }

        roleRepository.deleteById(id);
        return "Role deleted successfully";
    }

    private int getAccessLevel(String roleName) {
        switch (roleName.toLowerCase()) {
            case "customer":
                return 1;
            case "admin":
                return 2;
            case "admin+":
                return 3;
            default:
                return 0;
        }
    }
}