package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id: GrammarSymbol.java,v 1.1 2005/03/20 09:16:49 irmscher Exp $
 */

/**A GrammarSymbol is a generic symbol that can be
 * added to the symbol table for a grammar.
 */
abstract class GrammarSymbol {
    protected String id;

    public GrammarSymbol() {
    }

    public GrammarSymbol(String s) {
        id = s;
    }

    public String getId() {
        return id;
    }

    public void setId(String s) {
        id = s;
    }
}
