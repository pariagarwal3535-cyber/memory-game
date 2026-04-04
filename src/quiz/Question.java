package quiz;

/**
 * Represents a single quiz question with 4 options.
 * Used across all quiz modes and subjects.
 */
public class Question {

    public enum Difficulty { EASY, MEDIUM, HARD }
    public enum Subject {
        GK, ENGLISH, OPERATING_SYSTEMS, DATA_STRUCTURES,
        COMPUTER_NETWORKS, DBMS, OOP, ALGORITHMS
    }

    private int    id;
    private String question;
    private String[] options;   // Always 4 options
    private int    correctIndex; // 0-3
    private String explanation;
    private Subject subject;
    private Difficulty difficulty;

    public Question(int id, String question, String[] options,
                    int correctIndex, String explanation,
                    Subject subject, Difficulty difficulty) {
        this.id           = id;
        this.question     = question;
        this.options      = options;
        this.correctIndex = correctIndex;
        this.explanation  = explanation;
        this.subject      = subject;
        this.difficulty   = difficulty;
    }

    // Getters
    public int        getId()           { return id; }
    public String     getQuestion()     { return question; }
    public String[]   getOptions()      { return options; }
    public int        getCorrectIndex() { return correctIndex; }
    public String     getCorrectAnswer(){ return options[correctIndex]; }
    public String     getExplanation()  { return explanation; }
    public Subject    getSubject()      { return subject; }
    public Difficulty getDifficulty()   { return difficulty; }

    public boolean isCorrect(int selectedIndex) {
        return selectedIndex == correctIndex;
    }
}
