package onetoone.carson_systemTest.job;

import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.achievements.AchievementsController;
import onetoone.billing.BillingService;
import onetoone.config.GameConfig;
import onetoone.game.GameService;
import onetoone.job.JobController;
import onetoone.job.JobRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    private JobRunRepository runRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private ResourceRepository resourceRepo;
    @Mock
    private BillingService billingService;
    @Mock
    private GameService gameService;
    @Mock
    private AchievementsController achievementsController;
    @Mock
    private GameConfig config;

    @InjectMocks
    private JobController jobController;

    private User testUser;
    private Resource testResource;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setName("Test User");

        testResource = new Resource();
        testResource.setId(1);
        testResource.setUser(testUser);
        testResource.setTurnsLeft(10);
        testResource.setMoney(1000.0);
        
        // Mock GameConfig values (lenient since not all tests use them)
        lenient().when(config.getJobSoftCapPasses()).thenReturn(10);
        lenient().when(config.getJobSoftCapReduction()).thenReturn(0.4);
        lenient().when(config.getJobMinDurationMs()).thenReturn(5000L);
    }

    @Test
    void testGetConfig() {
        ResponseEntity<Map<String, Object>> response = jobController.getConfig();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(6.0, body.get("easyPayout"));
        assertEquals(10.0, body.get("mediumPayout"));
        assertEquals(16.0, body.get("hardPayout"));
    }

    @Test
    void testSubmitRun_NoTurns() {
        when(gameService.consumeTurn(1)).thenReturn(false);

        Map<String, Object> payload = new HashMap<>();
        payload.put("gameType", "PackingLine");
        payload.put("difficulty", "EASY");
        payload.put("score", 100);
        payload.put("passed", true);
        payload.put("durationMs", 30000L);
        payload.put("runNonce", "test-nonce");

        ResponseEntity<?> response = jobController.submitRun(1, payload);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    void testSubmitRun_TooShort() {
        when(gameService.consumeTurn(1)).thenReturn(true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("gameType", "PackingLine");
        payload.put("difficulty", "EASY");
        payload.put("score", 100);
        payload.put("passed", true);
        payload.put("durationMs", 1000L); // Too short
        payload.put("runNonce", "test-nonce");

        ResponseEntity<?> response = jobController.submitRun(1, payload);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
    }
}

