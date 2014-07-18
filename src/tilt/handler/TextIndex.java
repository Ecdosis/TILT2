/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.handler;
import java.util.ArrayList;
import java.util.StringTokenizer;
import calliope.AeseSpeller;

/**
 * An index into the uploaded HTML, recording the positions of each plain 
 * text word, number of words etc. Also we record the position and type of 
 * hyphens that precede newlines. To do this we need to lookup the
 * language.
 * @author desmond
 */
public class TextIndex 
{
    int numWords;
    String[] words;
    int[] indices;
    int[] hyphens;
    ArrayList<String> wordList;
    ArrayList<Integer> hyphenList;
    ArrayList<Integer> indexList;
    public static int NO_HYPHEN = 0;
    public static int SOFT_HYPHEN = 1;
    public static int HARD_HYPHEN = 2;
    int htmlIndex;
    AeseSpeller speller;
    String dict;
    private static String CAPUANA_HTML = 
    "<h3>Al lettore.</h3>\n<p>Quando l’autore e l’editore di quest"
    +"i due frammenti mi\npregarono di scrivere due parole di pref"
    +"azione per la pre-\nsente ristampa destinata ai bibliofili e"
    +" ai curiosi, io fran-\ncamente dissi: che mi sarebbe parso u"
    +"na bella impertinenza\nanche il solo pensiero di voler scimm"
    +"iottare il divino Aristofane:\nsi figurassero che cosa dovea"
    +" parermi l’averlo già fatto!</p>\n<p>Ma l’autore rispose che"
    +" appunto intendeva di esser da me\nscusato presso il pubblic"
    +"o di questa sua profanazione improv-\nvisata sotto l’impulso"
    +" d'una forza irresistibile.</p>\n<p>(Oh Dio, la forza irresi"
    +"stibile anche nell’arte! Dove s’andrà\na finire con essa?)</"
    +"p>\n<p>Almeno, replicai, avreste voi accarezzato un po’ di p"
    +"iù la\nforma! Aristofane, v’insegnerebbe il nostro Franchett"
    +"i che se\nn'intende meglio di tutti noialtri, Aristofane, an"
    +"che laddo-\nve sembra più negletto lavorava di fino e limava"
    +" e rilima-\nva...</p>\n<p>Bella forza! esclamò l’autore. Ma "
    +"allora non si tratterebbe\npiù d’una profanazione: come io c"
    +"onfesso...</p>\n<p>Una osservazione che mi chiuse la bocca. "
    +"E dovrebbe, credo,\nchiuderla anche a quelli che, lette le N"
    +"uove Rane,\nne daranno un giudizio pari al mio</p>\n";
    private static String DEROBERTO_1920_HTML = 
    "<!doctype HTML5>\n<html><head>\n<meta http-equiv=\"Content-Ty"
    +"pe\" content=\"text/html; charset=UTF-8\"></meta>\n<script t"
    +"ype=\"text/javascript\" src=\"http://code.jquery.com/jquery-"
    +"1.9.0.js\"></script>\n</head>\n<body>\n<p align=\"center\">–"
    +" 8 –</p>\n<p>che passo, non distinguendo nulla pel buio; ma "
    +"la voce\ndella principessa Margherita li guidò:\n<p>– «Don M"
    +"ariano!... Don Giacinto!...»</p>\n<p>– «Principessa!... Sign"
    +"ora mia!... Com'è stato?...\nE Lucrezia?... Consalvo?... La "
    +"bambina?...»\n<p>Il principino, seduto sopra uno sgabello, c"
    +"on le \ngambe penzoloni, le dondolava ritmicamente, guar-\nd"
    +"ando per aria a bocca aperta; di scosta, in un angolo \ndi d"
    +"ivano, Lucrezia stava ingrottata, con gli occhi \nasciutti.<"
    +"/p>\n<p>– «Ma com'è avvenuto, così a un tratto?» insi-\nstev"
    +"a don Mariano.</p>\n<p>E la principessa, aprendo le braccia:"
    +"</p>\n<p>– «Non so... non capisco... È arrivato Salvatore \n"
    +"dal Belvedere, con un biglietto del signor Marco... \nLì, su"
    +" quella tavola, guardate... Giacomino è partito su-\nbito.» "
    +"A bassa voce, rivolta a don Mariano, intanto \nche l'altro l"
    +"eggeva il biglietto: «Lucrezia voleva an-\ndare anche lei,» "
    +"aggiunse, «suo fratello ha detto di \nno... Che ci avrebbe f"
    +"atto?»</p>\n<p>– «Confusione di più!... Il principe ha avuto"
    +" ra-\ngione...»</p>\n<p>– «Niente!» annunziava frattanto don"
    +" Giacinto, \nfinito di leggere il biglietto. «Non spiega nie"
    +"nte!... \nE hanno avvertito gli altri... hanno dispacciato?."
    +"..»</p>\n<p>– «Io non so... Baldassarre...»</p>\n<p>– «Morir"
    +"e così, sola sola, senza un figlio, un pa-\nrente!» esclamav"
    +"a don Mariano, non potendo darsi \npace; ma don Giacinto:</p"
    +">\n<p>– «La colpa non è di questi qui, poveretti!... Essi \n"
    +"hanno la coscienza tranquilla.»</p>\n<p>– «Se ci avesse volu"
    +"ti...» cominciò la principessa, \ntimida mente, più piano di"
    +" prima; ma poi, quasi im-\npaurita, non finì la frase.</p>\n"
    +"<p>Don Mariano tirò un sospiro doloroso e andò a met-\ntersi"
    +" vicino alla signorina.</p>\n<p>– «Povera Lucrezia! Che disg"
    +"razia!... Avete ra-\ngione!... Ma fatevi animo!... Coraggio!"
    +"...»</p>\n</body>\n</html>\n";
    public TextIndex( String html, String lang ) throws Exception
    {
        speller = new AeseSpeller( lang );
        this.dict = lang;
        digest( html );
        speller.cleanup();
    }
    /**
     * Should we hard-hyphenate two words or part-words?
     * @param last the previous 'word'
     * @param next the word on the next line
     * @return true for a hard hyphen else soft
     */
    private boolean isHardHyphen( String last, String next )
    {
        String compound = last+next;
        if ( speller.hasWord(last,dict)
            &&speller.hasWord(next,dict)
            &&(!speller.hasWord(compound,dict)))
            return true;
        else
            return false;
    }
    /**
     * Add a word to the three lists
     * @param word the word to store
     * @param hyphen the type of hyphen
     */
    private void addWord( String word, int hyphen )
    {
        wordList.add( word );
        if ( !word.equals("-") )
            numWords++;
        indexList.add( htmlIndex );
        hyphenList.add( new Integer(hyphen) );
        int lastHyphen = hyphenList.get(hyphenList.size()-1).intValue();
        // check last hyphen status
        if ( lastHyphen == SOFT_HYPHEN && hyphen == NO_HYPHEN )
        {
            String prev = wordList.get(wordList.size()-2);
            if ( isHardHyphen(prev,word) )
                hyphenList.set(hyphenList.size()-1,new Integer(HARD_HYPHEN));
        }
    }
    /**
     * Extract the words from some html
     * @param html the text to strip
     * @param textIndex records where the individual strings were found
     * @return the text index (original + stripped plus indices)
     */
    void digest( String html )
    {
        StringTokenizer st = new StringTokenizer(html,"<> \n\t",true);
        int state = 0;
        htmlIndex = 0;
        wordList = new ArrayList<>();
        indexList = new ArrayList<>();
        hyphenList = new ArrayList<>();
        while ( st.hasMoreElements() )
        {
            String token = st.nextToken();
            switch ( state )
            {
                case 0: // looking for start-tag
                    if (token.equals("<") )
                        state = 1;
                    break;
                case 1: // reading start tag
                    if ( token.equals(">") )
                        state = 2;
                    else if ( token.toLowerCase().equals("head") )
                        state = 3;
                    break;
                case 2: // reading text
                    if ( !token.equals("<") )
                    {
                        if ( token.length() > 1 
                            || !Character.isWhitespace(token.charAt(0)) )
                        {
                            if ( token.endsWith("-") && token.length()>1 )
                            {
                                String hWord = token.substring(0,token.length()-1);
                                addWord( hWord, NO_HYPHEN );
                                addWord( "-", SOFT_HYPHEN);
                            }
                            else 
                                addWord( token,NO_HYPHEN );
                        }
                    }
                    else
                        state = 1;
                    break;
                case 3:
                    if ( token.equals("<") )
                        state = 4;
                    break;
                case 4:
                    if ( token.toLowerCase().equals("/head") )
                        state = 5;
                    break;
                case 5:
                    if ( token.equals(">") )
                        state = 0;
                    break;
            }
            htmlIndex += token.length();
        }
        hyphens = intArray( hyphenList );
        indices = intArray( indexList );
        words = strArray( wordList );
    }
    private int[] intArray( ArrayList<Integer> list )
    {
        int[] array = new int[list.size()];    
        for ( int i=0;i<list.size();i++ )
            array[i] = list.get(i).intValue();
        return array;
    }
    private String[] strArray( ArrayList<String> list )
    {
        String[] array = new String[list.size()];    
        list.toArray( array );
        return array;
    }
    /**
     * Get the number of plain text words
     * @return an int
     */
    public int numWords()
    {
        return numWords;
    }
    /**
     * Get the total number of characters on the page
     * @return an int
     */
    public int numChars()
    {
        int chars = 0;
        for ( int i=0;i<words.length;i++ )
        {
            chars += words[i].length();
        }
        return chars;
    }
    /**
     * Get the extracted words
     * @return an array of Strings
     */
    public String[] getWords()
    {
        return words;
    }
    /**
     * Get the offsets of the words into the original html
     * @return an array of int offsets
     */
    public int[] getTextOffsets()
    {
        return indices;
    }
    /**
     * Get the hyphens between words
     * @return an array of ints
     */
    public int[] getHyphens()
    {
        return hyphens;
    }
    /**
     * Get the widths of words (including hyphens at the end)
     * @param ppc the fractional number of pixels per char
     * @return an array of word widths
     */
    public int[] getWordWidths( float ppc )
    {
        int[] widths = new int[words.length];
        for ( int i=0;i<words.length;i++ )
        {
            String word = words[i];
            if ( hyphens[i] == SOFT_HYPHEN || hyphens[i] == HARD_HYPHEN )
                word += "-";
            widths[i] = Math.round((float)word.length()*ppc);
        }
        return widths;
    }
    public static void main( String[] args )
    {
        try
        {
            TextIndex ti = new TextIndex( DEROBERTO_1920_HTML, "en_GB" );
            String[] parole = ti.getWords();
            int[] offsets = ti.getTextOffsets();
            int[] hyphens = ti.getHyphens();
            for ( int i=0;i<parole.length;i++ )
            {
                String hyphen = "";
                if ( hyphens[i] != NO_HYPHEN )
                {
                    if ( hyphens[i]==SOFT_HYPHEN )
                        hyphen = "SOFT";
                    else if ( hyphens[i] == HARD_HYPHEN )
                        hyphen = "HARD";
                }
                System.out.println( parole[i]+" "+offsets[i]+" "+hyphen);
            }
            System.out.println("num words="+parole.length);
        }
        catch ( Exception e )
        {
            e.printStackTrace(System.out);
        }
    }
}
