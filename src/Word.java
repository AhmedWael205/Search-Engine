public class Word {
    public String text;
    public double TF;
    public String Pos;

    public Word(String T, String P)
    {
        this.text = T;
        this.TF = 1.0;
        this.Pos =  P;
    }
}
