package onetoone.chase_systemTest.library;

import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.achievements.AchievementsController;
import onetoone.billing.BillingService;
import onetoone.config.GameConfig;
import onetoone.game.GameService;
import onetoone.library.LibraryController;
import onetoone.library.LibraryQuestion;
import onetoone.library.LibraryQuestionRepository;
import onetoone.library.QuestionAttempt;
import onetoone.library.QuestionAttemptRepository;
import onetoone.util.ApiError;
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
class LibraryControllerTest {

    @Mock
    private LibraryQuestionRepository questionRepo;
    @Mock
    private QuestionAttemptRepository attemptRepo;
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
    private LibraryController libraryController;

    private User testUser;
    private Resource testResource;
    private LibraryQuestion testQuestion;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setName("Test User");

        testResource = new Resource();
        testResource.setId(1);
        testResource.setUser(testUser);
        testResource.setMoney(1000.0);

        testQuestion = new LibraryQuestion();
        testQuestion.setId(1L);
        testQuestion.setTopic("Credit Basics");
        testQuestion.setDifficulty("EASY");
        testQuestion.setPrompt("What is a credit score?");
        testQuestion.setChoices("[\"Option A\",\"Option B\",\"Option C\",\"Option D\"]");
        testQuestion.setCorrectIndex(0);
        testQuestion.setExplanation("Credit score measures creditworthiness.");
        
        // Mock GameConfig values (lenient since not all tests use them)
        lenient().when(config.getLibraryRewardMoney()).thenReturn(10.0);
        lenient().when(config.getLibraryRewardXp()).thenReturn(5); // Returns int, not double
        lenient().when(questionRepo.count()).thenReturn(1L); // Prevent seeding
    }

    @Test
    void testSubmitAttempt_FirstCorrect_GrantsReward() {
        when(gameService.consumeTurn(1)).thenReturn(true);
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(questionRepo.findById(1L)).thenReturn(Optional.of(testQuestion));
        when(attemptRepo.findByUser_IdAndQuestion_Id(1, 1L)).thenReturn(Optional.empty());
        when(resourceRepo.findByUserId(1)).thenReturn(testResource);
        when(attemptRepo.findByUser_IdAndIsMasteredTrue(1)).thenReturn(java.util.Collections.emptyList());

        Map<String, Object> payload = new HashMap<>();
        payload.put("questionId", 1L);
        payload.put("answerIndex", 0); // Correct answer

        ResponseEntity<?> response = libraryController.submitAttempt(1, payload);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("correct"));
        assertTrue((Boolean) body.get("cleared"));
        assertNotNull(body.get("reward"));
        
        verify(resourceRepo).save(any(Resource.class));
        verify(billingService).createTransaction(eq(1), anyString(), anyDouble(), anyString(), any());
    }

    @Test
    void testSubmitAttempt_SecondCorrect_NoReward() {
        when(gameService.consumeTurn(1)).thenReturn(true);
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(questionRepo.findById(1L)).thenReturn(Optional.of(testQuestion));
        
        QuestionAttempt existingAttempt = new QuestionAttempt();
        existingAttempt.setMastered(true);
        when(attemptRepo.findByUser_IdAndQuestion_Id(1, 1L)).thenReturn(Optional.of(existingAttempt));

        Map<String, Object> payload = new HashMap<>();
        payload.put("questionId", 1L);
        payload.put("answerIndex", 0); // Correct answer

        ResponseEntity<?> response = libraryController.submitAttempt(1, payload);

        assertNotNull(response);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("correct"));
        assertFalse((Boolean) body.get("cleared"));
        assertNull(body.get("reward"));
    }

    @Test
    void testSubmitAttempt_NoTurns() {
        when(gameService.consumeTurn(1)).thenReturn(false);

        Map<String, Object> payload = new HashMap<>();
        payload.put("questionId", 1L);
        payload.put("answerIndex", 0);

        ResponseEntity<?> response = libraryController.submitAttempt(1, payload);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
        ApiError error = (ApiError) response.getBody();
        assertNotNull(error);
        assertEquals("NO_TURNS", error.getError());
    }
}

