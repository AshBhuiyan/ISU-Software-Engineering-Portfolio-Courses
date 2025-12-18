package onetoone.chase_systemTest.wellness;

import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.achievements.AchievementsController;
import onetoone.billing.BillingService;
import onetoone.config.GameConfig;
import onetoone.game.GameService;
import onetoone.util.ApiError;
import onetoone.wellness.ChallengeEnrollment;
import onetoone.wellness.ChallengeEnrollmentRepository;
import onetoone.wellness.WellnessChallenge;
import onetoone.wellness.WellnessChallengeRepository;
import onetoone.wellness.WellnessController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WellnessControllerTest {

    @Mock
    private WellnessChallengeRepository challengeRepo;
    @Mock
    private ChallengeEnrollmentRepository enrollmentRepo;
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
    private WellnessController wellnessController;

    private User testUser;
    private Resource testResource;
    private WellnessChallenge testChallenge;
    private ChallengeEnrollment testEnrollment;

    @BeforeEach
    void setUp() {
        // Mock GameConfig before controller initialization (lenient since not all tests use them)
        lenient().when(config.getWellnessRewardMoney()).thenReturn(15.0);
        lenient().when(challengeRepo.count()).thenReturn(1L); // Prevent seeding
        
        testUser = new User();
        testUser.setId(1);
        testUser.setName("Test User");

        testResource = new Resource();
        testResource.setId(1);
        testResource.setUser(testUser);
        testResource.setMoney(1000.0);

        testChallenge = new WellnessChallenge();
        testChallenge.setId(1L);
        testChallenge.setTitle("Weekly Challenge");
        testChallenge.setTargetCount(3);
        testChallenge.setRewardCash(15.0);
        testChallenge.setRewardXp(15.0);

        testEnrollment = new ChallengeEnrollment();
        testEnrollment.setId(1L);
        testEnrollment.setUser(testUser);
        testEnrollment.setChallenge(testChallenge);
        testEnrollment.setProgressCount(0);
        testEnrollment.setTargetCount(3);
        testEnrollment.setStatus("ENROLLED");
    }

    @Test
    void testSubmitProgress_IncrementsProgress() {
        when(enrollmentRepo.findById(1L)).thenReturn(Optional.of(testEnrollment));

        Map<String, Object> payload = new HashMap<>();
        payload.put("passed", true);

        ResponseEntity<?> response = wellnessController.submitProgress(1L, 1, payload);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(1, body.get("progressCount"));
        verify(enrollmentRepo).save(any(ChallengeEnrollment.class));
    }

    @Test
    void testSubmitProgress_Fail_NoIncrement() {
        when(enrollmentRepo.findById(1L)).thenReturn(Optional.of(testEnrollment));

        Map<String, Object> payload = new HashMap<>();
        payload.put("passed", false);

        ResponseEntity<?> response = wellnessController.submitProgress(1L, 1, payload);

        assertNotNull(response);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(0, body.get("progressCount")); // No increment
    }

    @Test
    void testClaimReward_FirstTime_GrantsReward() {
        testEnrollment.setProgressCount(3);
        testEnrollment.setTargetCount(3);
        testEnrollment.setStatus("COMPLETED");
        
        when(enrollmentRepo.findById(1L)).thenReturn(Optional.of(testEnrollment));
        when(resourceRepo.findByUserId(1)).thenReturn(testResource);

        ResponseEntity<?> response = wellnessController.claimReward(1L, 1);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(15.0, body.get("rewardCash"));
        assertEquals("Wellness Champion", body.get("badge"));
        
        verify(resourceRepo).save(any(Resource.class));
        verify(billingService).createTransaction(eq(1), anyString(), anyDouble(), anyString(), any());
    }

    @Test
    void testClaimReward_AlreadyClaimed_Idempotent() {
        testEnrollment.setStatus("CLAIMED");
        
        when(enrollmentRepo.findById(1L)).thenReturn(Optional.of(testEnrollment));

        ResponseEntity<?> response = wellnessController.claimReward(1L, 1);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("alreadyClaimed"));
        assertEquals(15.0, body.get("rewardCash"));
        
        // Should not grant rewards again
        verify(resourceRepo, never()).save(any(Resource.class));
    }

    @Test
    void testClaimReward_NotCompleted_Error() {
        testEnrollment.setStatus("ENROLLED");
        testEnrollment.setProgressCount(1);
        testEnrollment.setTargetCount(3);
        
        when(enrollmentRepo.findById(1L)).thenReturn(Optional.of(testEnrollment));

        ResponseEntity<?> response = wellnessController.claimReward(1L, 1);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
        ApiError error = (ApiError) response.getBody();
        assertNotNull(error);
        assertEquals("NOT_COMPLETED", error.getError());
    }
}

