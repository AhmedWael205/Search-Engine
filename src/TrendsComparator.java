import java.util.Comparator;

public class TrendsComparator implements Comparator<TrendsResult> {

    @Override
    public int compare(TrendsResult o1, TrendsResult o2) {
        int f1 = o1.Freq;
        int f2 = o2.Freq;
        int compareTo = 0;
        if(f1 == f2)
        {
            compareTo = 0;
        }
        else if(f1 > f2)
        {
            compareTo = -1;
        }
        else if(f1 < f2)
        {
            compareTo = 1;
        }
        return compareTo;
    }
}
