package cycredit.io;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.android.material.card.MaterialCardView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cycredit.io.guilds.ApiClient;
import cycredit.io.util.ErrorHandler;

public class QuizActivity extends AppCompatActivity {

    private static final String BASE_URL = ApiClient.BASE_URL;

    private int userId;

    private SwipeRefreshLayout refresher;
    private Spinner topicSpinner;
    private Spinner difficultySpinner;
    private Button refreshButton;
    private MaterialCardView questionContainer;
    private TextView questionTopic;
    private TextView questionText;
    private RadioGroup optionsGroup;
    private Button submitBtn;
    private Button nextBtn;
    private TextView resultText;
    private TextView rewardText;
    private TextView emptyState;
    private ProgressBar progressBar;
    private TextView progressSummary;

    private final List<Question> masterQuestions = new ArrayList<>();
    private final List<Question> filteredQuestions = new ArrayList<>();
    private int currentIndex = -1;
    private boolean awaitingSubmission = false;

    private static class Question {
        long id;
        String topic;
        String difficulty;
        String prompt;
        List<String> choices;
        String explanation;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Parks Library – Quiz");
        }

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId <= 0) {
            userId = Session.getUserId(this);
        }
        
        // Defensive check for required extras
        if (userId <= 0) {
            Toast.makeText(this, "Missing user data. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        setupListeners();
        setupBackButton();

        refresher.setRefreshing(true);
        fetchProgress();
        fetchMasterQuestions();
    }
    
    private void setupBackButton() {
        Button backBtn = findViewById(R.id.btn_back_to_map);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
    }

    private void bindViews() {
        refresher = findViewById(R.id.swipe_refresh);
        topicSpinner = findViewById(R.id.topic_spinner);
        difficultySpinner = findViewById(R.id.difficulty_spinner);
        refreshButton = findViewById(R.id.refresh_button);
        questionContainer = findViewById(R.id.question_container);
        questionTopic = findViewById(R.id.question_topic);
        questionText = findViewById(R.id.question_text);
        optionsGroup = findViewById(R.id.quiz_options);
        submitBtn = findViewById(R.id.submit_quiz_btn);
        nextBtn = findViewById(R.id.next_question_btn);
        resultText = findViewById(R.id.result_text);
        rewardText = findViewById(R.id.reward_text);
        emptyState = findViewById(R.id.empty_state);
        progressBar = findViewById(R.id.progress_bar);
        progressSummary = findViewById(R.id.progress_summary);
    }

    private void setupListeners() {
        topicSpinner.setOnItemSelectedListener(new SimpleSelectionListener(this::applyFilters));
        difficultySpinner.setOnItemSelectedListener(new SimpleSelectionListener(this::applyFilters));

        refresher.setOnRefreshListener(() -> {
            fetchProgress();
            fetchMasterQuestions();
        });

        refreshButton.setOnClickListener(v -> applyFilters());

        submitBtn.setOnClickListener(v -> submitCurrentQuestion());
        nextBtn.setOnClickListener(v -> {
            if (currentIndex < filteredQuestions.size() - 1) {
                currentIndex++;
                showCurrentQuestion();
            } else {
                Toast.makeText(this, "No more questions for this filter.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class SimpleSelectionListener implements AdapterView.OnItemSelectedListener {
        private final Runnable onChange;

        SimpleSelectionListener(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (onChange != null) onChange.run();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // no-op
        }
    }

    private void fetchMasterQuestions() {
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                BASE_URL + "/library/questions",
                null,
                response -> {
                    try {
                        masterQuestions.clear();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            masterQuestions.add(parseQuestion(obj));
                        }
                        updateFilters();
                        applyFilters();
                        cycredit.io.util.ToastHelper.showList(this, "questions", masterQuestions.size());
                    } catch (JSONException e) {
                        cycredit.io.util.ToastHelper.showError(this, "Parse questions", e.getMessage());
                    } finally {
                        refresher.setRefreshing(false);
                    }
                },
                error -> {
                    refresher.setRefreshing(false);
                    cycredit.io.util.ToastHelper.showError(this, "Fetch questions", cycredit.io.util.ErrorHandler.getErrorMessage(error));
                    updateVisibilityForEmpty();
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private void updateFilters() {
        Set<String> topics = new LinkedHashSet<>();
        Set<String> difficulties = new LinkedHashSet<>();
        topics.add("All");
        difficulties.add("All");
        for (Question q : masterQuestions) {
            if (q.topic != null) topics.add(q.topic);
            if (q.difficulty != null) difficulties.add(q.difficulty);
        }

        ArrayAdapter<String> topicAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(topics));
        topicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        topicSpinner.setAdapter(topicAdapter);

        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(difficulties));
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(difficultyAdapter);
    }

    private void applyFilters() {
        String topicSelection = (String) topicSpinner.getSelectedItem();
        String difficultySelection = (String) difficultySpinner.getSelectedItem();

        filteredQuestions.clear();

        for (Question question : masterQuestions) {
            boolean matchesTopic = topicSelection == null
                    || "All".equalsIgnoreCase(topicSelection)
                    || (question.topic != null && question.topic.equalsIgnoreCase(topicSelection));
            boolean matchesDifficulty = difficultySelection == null
                    || "All".equalsIgnoreCase(difficultySelection)
                    || (question.difficulty != null && question.difficulty.equalsIgnoreCase(difficultySelection));
            if (matchesTopic && matchesDifficulty) {
                filteredQuestions.add(question);
            }
        }

        currentIndex = filteredQuestions.isEmpty() ? -1 : 0;
        showCurrentQuestion();
    }

    private void showCurrentQuestion() {
        if (currentIndex < 0 || currentIndex >= filteredQuestions.size()) {
            updateVisibilityForEmpty();
            return;
        }

        Question question = filteredQuestions.get(currentIndex);
        awaitingSubmission = true;
        questionContainer.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        optionsGroup.removeAllViews();
        resultText.setVisibility(View.GONE);
        rewardText.setVisibility(View.GONE);
        nextBtn.setVisibility(View.GONE);
        submitBtn.setEnabled(true);

        questionTopic.setText(String.format(Locale.US, "%s • %s", question.topic, question.difficulty));
        questionText.setText(question.prompt);

        for (int i = 0; i < question.choices.size(); i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(View.generateViewId());
            radioButton.setText(question.choices.get(i));
            radioButton.setTag(i);
            // Style for dark theme - white text
            radioButton.setTextColor(0xFFFFFFFF); // White
            radioButton.setTextSize(16f);
            radioButton.setPadding(16, 16, 16, 16);
            radioButton.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF00C8FF)); // Cyan accent
            optionsGroup.addView(radioButton);
        }
    }

    private void updateVisibilityForEmpty() {
        questionContainer.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    private void submitCurrentQuestion() {
        if (!awaitingSubmission) {
            Toast.makeText(this, "Question already submitted. Use Next.", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = optionsGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select an answer.", Toast.LENGTH_SHORT).show();
            return;
        }

        Question question = filteredQuestions.get(currentIndex);
        View selectedView = optionsGroup.findViewById(selectedId);
        int answerIndex = (int) selectedView.getTag();

        progressBar.setVisibility(View.VISIBLE);
        submitBtn.setEnabled(false);

        JSONObject body = new JSONObject();
        try {
            body.put("questionId", question.id);
            body.put("answerIndex", answerIndex);
        } catch (JSONException e) {
            Toast.makeText(this, "Failed to build request", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            submitBtn.setEnabled(true);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                BASE_URL + "/library/attempts?userId=" + userId,
                body,
                response -> {
                    handleAttemptResponse(question, response);
                    cycredit.io.util.ToastHelper.showCreate(this, "quiz attempt");
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    submitBtn.setEnabled(true);
                    ErrorHandler.handleError(this, error, "Submit failed");
                    cycredit.io.util.ToastHelper.showError(this, "Submit answer", cycredit.io.util.ErrorHandler.getErrorMessage(error));
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private void handleAttemptResponse(Question question, JSONObject response) {
        progressBar.setVisibility(View.GONE);
        submitBtn.setEnabled(false);
        awaitingSubmission = false;

        boolean correct = response.optBoolean("correct", false);
        boolean cleared = response.optBoolean("cleared", false);
        JSONObject reward = response.optJSONObject("reward");

        String explanation = response.optString("explanation", question.explanation);
        StringBuilder resultBuilder = new StringBuilder();

        if (correct) {
            resultBuilder.append("✅ Correct! ");
            if (cleared && reward == null) {
                resultBuilder.append("Already mastered – no additional reward.");
            }
            resultText.setTextColor(0xFF00FF88); // Green for correct
        } else {
            resultBuilder.append("❌ Not quite. Try reviewing the explanation and attempt another question.");
            resultText.setTextColor(0xFFE53935); // Red for incorrect
        }

        if (explanation != null && !explanation.isEmpty()) {
            resultBuilder.append("\n\nExplanation: ").append(explanation);
        }

        resultText.setText(resultBuilder.toString());
        resultText.setTextSize(15f);
        resultText.setVisibility(View.VISIBLE);

        if (reward != null) {
            double cash = reward.optDouble("cash", 0);
            double xp = reward.optDouble("xp", 0);
            rewardText.setVisibility(View.VISIBLE);
            rewardText.setText(String.format(Locale.US, "Reward: +$%.2f, +%.0f XP", cash, xp));
            Toast.makeText(this, String.format(Locale.US, "✅ Correct! +$%.2f, +%.0f XP", cash, xp), Toast.LENGTH_SHORT).show();
            HudSyncHelper.refreshHud(this, userId);
        } else {
            rewardText.setVisibility(View.GONE);
            if (correct) {
                Toast.makeText(this, "✅ Correct answer!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Incorrect. Try again!", Toast.LENGTH_SHORT).show();
            }
        }

        nextBtn.setVisibility(View.VISIBLE);
        fetchProgress();
    }

    private void fetchProgress() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                BASE_URL + "/library/progress?userId=" + userId,
                null,
                response -> {
                    int total = response.optInt("totalQuestions", masterQuestions.size());
                    int mastered = response.optInt("masteredQuestions", 0);
                    progressSummary.setText(String.format(Locale.US, "Mastered %d of %d questions", mastered, total));
                    cycredit.io.util.ToastHelper.showRead(this, "quiz progress", 1);
                },
                error -> {
                    progressSummary.setText("Progress unavailable");
                    cycredit.io.util.ToastHelper.showError(this, "Fetch progress", cycredit.io.util.ErrorHandler.getErrorMessage(error));
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private Question parseQuestion(JSONObject obj) throws JSONException {
        Question question = new Question();
        question.id = obj.getLong("id");
        question.topic = obj.optString("topic", "General");
        question.difficulty = obj.optString("difficulty", "Unknown");
        question.prompt = obj.optString("prompt", "");
        question.explanation = obj.optString("explanation", "");
        JSONArray choicesJson = obj.optJSONArray("choices");
        question.choices = new ArrayList<>();
        if (choicesJson != null) {
            for (int i = 0; i < choicesJson.length(); i++) {
                question.choices.add(choicesJson.optString(i));
            }
        }
        return question;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

