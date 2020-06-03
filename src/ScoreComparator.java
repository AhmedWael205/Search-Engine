import java.util.Comparator;

public class ScoreComparator implements Comparator<URLResult> {

    @Override
    public int compare(URLResult o1, URLResult o2) {
        return (o2.score > o1.score? 1 :
                (o2.score == o1.score ? 0 : -1));
    }
}
