package onetoone.Location;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class LocationController {

    @Autowired
    private LocationRepository repo;
    @Operation(
            summary = "Fetch all locations",
            description = "Returns a list of all locations in the system."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Locations fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/locations")
    public List<Location> all() {
        return repo.findAll();
    }
    @Operation(
            summary = "Fetch a location by ID",
            description = "Returns a single location based on its unique ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location found"),
            @ApiResponse(responseCode = "404", description = "Location not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/locations/{id}")
    public Location one(@PathVariable int id) {
        return repo.findById(id);
    }
    @Operation(
            summary = "Fetch locations by category",
            description = "Returns all locations that match the given category (case-insensitive)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Locations fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/locations/category/{cat}")
    public List<Location> byCategory(@PathVariable String cat) {
        return repo.findByCategoryIgnoreCase(cat);
    }
    @Operation(
            summary = "Create a new location",
            description = "Creates a new location using the provided request body."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/locations")
    public Location create(@RequestBody Location loc) {
        return repo.save(loc);
    }
    @Operation(
            summary = "Update a location",
            description = "Updates an existing location's fields based on the provided ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location updated successfully"),
            @ApiResponse(responseCode = "404", description = "Location not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PutMapping("/locations/{id}")
    public Location update(@PathVariable int id, @RequestBody Location up) {
        Location cur = repo.findById(id);
        if (cur == null) return null;
        cur.setName(up.getName());
        cur.setCategory(up.getCategory());
        cur.setXPercent(up.getXPercent());
        cur.setYPercent(up.getYPercent());
        cur.setDescription(up.getDescription());
        cur.setImageUrl(up.getImageUrl());
        return repo.save(cur);
    }
    @Operation(
            summary = "Delete a location",
            description = "Deletes a location by its unique ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Location not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @DeleteMapping("/locations/{id}")
    public String delete(@PathVariable int id) {
        repo.deleteById(id);
        return "deleted";
    }
}
