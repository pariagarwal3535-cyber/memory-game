package quiz;

import java.util.List;

/**
 * Controls quiz session state.
 * Tracks current question, score, time, and streak.
 */
public class QuizController {

    private List<Question> questions;
    private int currentIndex = 0;
    private int score        = 0;
    private int correct      = 0;
    private int incorrect    = 0;
    private int streak       = 0;
    private int maxStreak    = 0;
    private long startTime;
    private long[] questionTimes; // time taken per question in ms

    public QuizController(List<Question> questions) {
        this.questions     = questions;
        this.questionTimes = new long[questions.size()];
        this.startTime     = System.currentTimeMillis();
    }

    public void startQuestion() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Submit an answer. Returns true if correct.
     * Score = base points + speed bonus + streak bonus
     */
    public boolean submitAnswer(int selectedIndex) {
        long timeTaken = System.currentTimeMillis() - startTime;
        if (currentIndex < questionTimes.length)
            questionTimes[currentIndex] = timeTaken;

        Question q = getCurrentQuestion();
        boolean isCorrect = q.isCorrect(selectedIndex);

        if (isCorrect) {
            correct++;
            streak++;
            if (streak > maxStreak) maxStreak = streak;

            // Base score: 100 points
            int points = 100;
            // Speed bonus: up to 50 extra points if answered in under 5 seconds
            int speedBonus = (int) Math.max(0, 50 - (timeTaken / 100));
            // Streak bonus: 10 per consecutive correct
            int streakBonus = (streak - 1) * 10;
            score += points + speedBonus + streakBonus;
        } else {
            incorrect++;
            streak = 0;
        }

        currentIndex++;
        return isCorrect;
    }

    public boolean hasNext()          { return currentIndex < questions.size(); }
    public Question getCurrentQuestion() {
        if (currentIndex >= questions.size()) return null;
        return questions.get(currentIndex);
    }
    public int getCurrentIndex()      { return currentIndex; }
    public int getTotalQuestions()    { return questions.size(); }
    public int getScore()             { return score; }
    public int getCorrect()           { return correct; }
    public int getIncorrect()         { return incorrect; }
    public int getStreak()            { return streak; }
    public int getMaxStreak()         { return maxStreak; }
    public double getAccuracy() {
        int total = correct + incorrect;
        return total == 0 ? 0 : (correct * 100.0 / total);
    }
    public long getTotalTime() {
        return System.currentTimeMillis() - startTime;
    }
}