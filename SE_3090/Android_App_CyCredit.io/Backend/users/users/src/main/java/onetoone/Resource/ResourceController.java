package onetoone.Resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
    public class ResourceController {

        @Autowired
        private ResourceRepository resourceRepository;

        @Autowired
        private UserRepository userRepository;



    @Operation(
            summary = "Get resources for a user",
            description = "Fetches the current resource object for the specified user ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource found and returned"),
            @ApiResponse(responseCode = "404", description = "User or resource not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
        @GetMapping("/resource/{userId}")
        public Resource getResourceById(@PathVariable int userId) {
            return resourceRepository.findByUserId(userId);

        }

    @Operation(
            summary = "Create or update a user's resource",
            description = "Creates a new resource or updates an existing one with the specified money and credit values."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource created or updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/resource/{userId}/{money}/{credit}")
    public String createOrUpdateResource(
            @PathVariable int userId,
            @PathVariable double money,
            @PathVariable double credit) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));


        Resource resource = resourceRepository.findByUserId(userId);

        if (resource == null) {
            // No resource yet — create a new one
            resource = new Resource(5, money, credit);
            resource.setUser(user);
            user.setResource(resource);
        } else {
            // Resource exists — just update values
            resource.setMoney(money);
            resource.setCredit(credit);
            resource.setTurnsLeft(5);
        }

        resourceRepository.save(resource);
        return "Resource created or updated";
    }

    @Operation(
            summary = "Update a resource by ID",
            description = "Updates the money, credit, and turnsLeft of an existing resource."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource updated successfully"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
        @PutMapping("/resource/{id}")
        public Resource updateResource(@PathVariable int id, @RequestBody Resource updatedResource) {
            Resource resource = resourceRepository.findById(id);
            if (resource == null) {
                throw new RuntimeException("Resource not found");
            }
            // Update only turnsLeft, money, and credit (match old behavior exactly)
            // Preserve existing creditLimit and currentMonth values
            resource.setTurnsLeft(updatedResource.getTurnsLeft());
            resource.setMoney(updatedResource.getMoney());
            resource.setCredit(updatedResource.getCredit());
            // Don't update creditLimit and currentMonth - preserve existing values

            return resourceRepository.save(resource);
        }

    @Operation(
            summary = "Delete a resource by ID",
            description = "Deletes the resource with the specified ID and unlinks it from any user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @DeleteMapping("/resource/{id}")

    public String deleteResource(@PathVariable int id) {
        if (!resourceRepository.existsById(id)) {
            return "Resource not found";
        }

        // Unlink users using this resource
        Resource resource = resourceRepository.findById(id);
        if (resource != null) {
            User user = userRepository.findByResourceId(id);
            user.setResource(null);
            resourceRepository.delete(resource);
        }

        return "Resource deleted successfully";
    }
    }

