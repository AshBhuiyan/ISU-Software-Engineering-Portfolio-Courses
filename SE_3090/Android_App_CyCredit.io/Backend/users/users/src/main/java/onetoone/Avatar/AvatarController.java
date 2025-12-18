package onetoone.Avatar;

import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
public class AvatarController {

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private UserRepository userRepository;

    @Operation(
            summary = "Get avatar by user ID",
            description = "Fetches the avatar associated with a given user ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Avatar not found for the given user ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/avatar/{userId}")
    public Avatar getAvatarByUser(@PathVariable int userId) {
        return avatarRepository.findByUserId(userId);
    }
    @Operation(
            summary = "Create or update a user's avatar",
            description = "Creates a new avatar for a user if none exists, or updates the avatar name if it already exists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar created or updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/avatar/{userId}/{avatarName}")
    public String createOrUpdateAvatar(@PathVariable int userId, @PathVariable String avatarName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Avatar avatar = avatarRepository.findByUserId(userId);
        if (avatar == null) {
            avatar = new Avatar(avatarName, "New character created: " + avatarName + " - ready to join the game!");
            avatar.setUser(user);
            user.setAvatar(avatar);
        } else {
            avatar.setAvatarName(avatarName);
        }

        avatarRepository.save(avatar);
        userRepository.save(user);
        return "Avatar assigned: " + avatarName;
    }
    @Operation(
            summary = "Update an avatar's name",
            description = "Updates the name of an existing avatar using its avatar ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar updated successfully"),
            @ApiResponse(responseCode = "404", description = "Avatar not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/avatar/{id}/{newName}")
    public String updateAvatar(@PathVariable int id, @PathVariable String newName) {
        Avatar avatar = avatarRepository.findById(id);
        if (avatar == null) return "Avatar not found";

        avatar.setAvatarName(newName);
        avatarRepository.save(avatar);
        return "Avatar updated successfully: " + newName;
    }
    @Operation(
            summary = "Delete an avatar",
            description = "Deletes an avatar using its ID and clears the association from the user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Avatar not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/avatar/{id}")
    public String deleteAvatar(@PathVariable int id) {
        Avatar avatar = avatarRepository.findById(id);
        if (avatar == null) return "Avatar not found";

        User user = avatar.getUser();
        if (user != null) {
            user.setAvatar(null);
            userRepository.save(user);
        }

        avatarRepository.delete(avatar);
        return "Avatar deleted successfully";
    }
}