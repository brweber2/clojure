/*
 * $Id$
 * Copyright (C) 2010 brweber2
 */
package clojure.lang;

import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bweber
 * @version $Rev$, $Date$
 */
public class AlternateReader
{

    public static final String FILE_EXTENSION = ".bbw";

    static IFn[] macros = new IFn[256];

    static
    {
        macros['"'] = new LispReader.StringReader();
        macros[';'] = new LispReader.CommentReader();
        macros['\\'] = new LispReader.CharacterReader();
    }


    static private IFn getMacro( int ch )
    {
        if ( ch < macros.length )
        {
            return macros[ch];
        }
        return null;
    }

    static public Object read( PushbackReader r, boolean eofIsError, Object eofValue, boolean isRecursive )
            throws Exception
    {
//        System.out.println( "Attempting to read an alternate lisp object." );

        for (; ; )
        {
            skipWhitespace( r );

            int ch = r.read();

//            System.out.println( "read char " + ( char ) ch );

            if ( ch == -1 )
            {
                if ( eofIsError )
                {
                    throw new Exception( "EOF while reading" );
                }
                else
                {
                    return eofValue;
                }
            }

            if ( Character.isDigit( ch ) )
            {
                Object n = LispReader.readNumber( r, ( char ) ch );
                if ( RT.suppressRead() )
                {
                    return null;
                }
                return n;
            }

            IFn macroFn = getMacro( ch );
            if ( macroFn != null )
            {
//                System.out.println( "found macro function: " + macroFn );
                Object ret = macroFn.invoke( r, ( char ) ch );
                if ( RT.suppressRead() )
                {
                    return null;
                }
                //no op macros return the reader
                if ( ret == r )
                {
                    continue;
                }
                return ret;
            }

            if ( ch == '+' || ch == '-' )
            {
                int ch2 = r.read();
                if ( Character.isDigit( ch2 ) )
                {
                    LispReader.unread( r, ch2 );
                    Object n = LispReader.readNumber( r, ( char ) ch );
                    if ( RT.suppressRead() )
                    {
                        return null;
                    }
                    return n;
                }
                LispReader.unread( r, ch2 );
            }

            LispReader.unread( r, ch );

            if ( peekFor( "}", r ) || peekFor( ")", r ) || peekFor( ">", r ) || peekFor( "]", r ) )
            {
                return null;
            }

            if ( peekFor( "namespace", r ) )
            {
                return readNamespace( r );
            }

            if ( peekFor( "function", r ) )
            {
                return readFunction( r );
            }

            if ( peekFor( "new", r ) )
            {
                return readInstantiation( r );
            }

            if ( peekFor( "val", r ) )
            {
                return readValue( r );
            }

            if ( peekFor( "{", r ) )
            {
                return readBlock( r );
            }

            if ( peekFor( "#(", r ) )
            {
                return getList( r );
            }

            if ( peekFor( "#[", r ) )
            {
                return getVector( r );
            }

            if ( peekFor( "#<", r ) )
            {
                return getSet( r );
            }

            if ( peekFor( "#{", r ) )
            {
                return getMap( r );
            }

            Object invocation = readInvocation( r );
            if ( invocation != null )
            {
                return invocation;
            }

            String token = LispReader.readToken( r, ( char ) ch );
            if ( RT.suppressRead() )
            {
                return null;
            }
            return LispReader.interpretToken( token );
        }
    }

    public static int skipWhitespace( PushbackReader r ) throws Exception
    {
        int ch = r.read();
        int count = 0;
        while ( LispReader.isWhitespace( ch ) )
        {
            ch = r.read();
            count++;
        }
        LispReader.unread( r, ch );
        return count;
    }

    public static Object readName( PushbackReader r ) throws Exception
    {
        String token = LispReader.readToken( r, ( char ) r.read() );
//        System.out.println( "read name " + token );
        return LispReader.matchSymbol( token );
    }

    public static void unreadString( String str, PushbackReader r ) throws Exception
    {
        char[] readChars = str.toCharArray();
        int[] reversed = new int[readChars.length];
        for ( int i = 0; i < readChars.length; i++ )
        {
            reversed[i] = readChars[readChars.length - 1 - i];
        }
        for ( int readChar : reversed )
        {
            LispReader.unread( r, readChar );
        }
    }

    public static boolean peekFor( String keyword, PushbackReader r ) throws Exception
    {
        StringBuilder readSoFar = new StringBuilder();
        for ( int index = 0; index < keyword.length(); index++ )
        {
            int ch = r.read();
            readSoFar.append( ( char ) ch );
            if ( keyword.charAt( index ) != ( char ) ch )
            {
                unreadString( readSoFar.toString(), r );
                return false;
            }
        }
        unreadString( readSoFar.toString(), r );
        return true;
    }

    public static boolean readKeyword( String keyword, PushbackReader r ) throws Exception
    {
        StringBuilder readSoFar = new StringBuilder();
        for ( int index = 0; index < keyword.length(); index++ )
        {
            int ch = r.read();
            readSoFar.append( ( char ) ch );
            if ( keyword.charAt( index ) != ( char ) ch )
            {
                unreadString( readSoFar.toString(), r );
                return false;
            }
        }
        return true;
    }

    static public Object readNamespace( PushbackReader r ) throws Exception
    {
//        System.out.println( "attempting to read namespace" );
        skipWhitespace( r );
        readKeyword( "namespace", r );
        skipWhitespace( r );

        Object name = readName( r );
//        System.out.println( "found namespace " + name );
        return readNamespace( name );
    }

    static public Object readNamespace( Object s )
    {
        Symbol ks = Symbol.intern( s.toString() );
        Namespace kns = Compiler.namespaceFor( ks );
        return Keyword.intern( kns.name.name, ks.name );
    }

    static public Object readValue( PushbackReader r ) throws Exception
    {
//        System.out.println( "attempting to read value" );
        skipWhitespace( r );
        readKeyword( "val", r );
        skipWhitespace( r );
        Object variable = readName( r );
        skipWhitespace( r );
        readKeyword( "=", r );
        skipWhitespace( r );
        Object expression = readExpression( r );

        // process until the end of the current block
        Object[] exprs = readExpressions( r, "}" );
        // trick our block reader into reading just the end...
        unreadString( "}", r );
        
        ISeq result = RT.listStar( Compiler.LET, RT.vector( variable, expression ), RT.arrayToList( exprs ) );

//        System.out.println( "done reading value" );
        // (let [variable expression] ... )
        return result;
    }

    static public Object[] readParams( PushbackReader r ) throws Exception
    {
//        System.out.println( "attempting to read params" );
        List params = new ArrayList();
        readKeyword( "[", r );
        skipWhitespace( r );

        Object theType = readName( r );
        while ( !"]".equals( ( ( Symbol ) theType ).getName() ) )
        {
            skipWhitespace( r );
            Object name = readName( r );
            skipWhitespace( r );
            ( ( Symbol ) name ).withMeta( RT.map( RT.TAG_KEY, theType ) );
            params.add( name );
//            System.out.println( "read param: " + theType + ":" + name );
            theType = readName( r );
        }
//        System.out.println( "done reading params" );
        return params.toArray();
    }

    static public Object[] readExpressions( PushbackReader r, String endToken ) throws Exception
    {
//        System.out.println( "attempting to read expressions" );
        List<Object> expressions = new ArrayList<Object>();
        while ( !readKeyword( endToken, r ) )
        {
//            System.out.println( "attempting to read expression" );
            skipWhitespace( r );
            Object expr = readExpression( r );
//            System.out.println( "read expression: " + expr );
            if ( expr == null )
            {
//                System.out.println( "found null expr" );
                continue;
            }
            else
            {
//                System.out.println( "adding normal expression " + expr );
                expressions.add( expr );
            }
//            System.out.println( "read an expression" );
        }
//        System.out.println( "done reading expressions" );
        return expressions.toArray();
    }

    static public Object readExpression( PushbackReader r ) throws Exception
    {
        return LispReader.read( r, true, null, false, FILE_EXTENSION );
    }

    static public Object readInstantiation( PushbackReader r ) throws Exception
    {
//        System.out.println( "attempting to read instantiation" );
        skipWhitespace( r );
        readKeyword( "new", r );
        skipWhitespace( r );
        Object className = readName( r );
        skipWhitespace( r );
        readKeyword( "(", r );
        Object[] args = readExpressions( r, ")" );

//        System.out.println( "done reading instantiation" );

        return RT.listStar( className + ".", RT.arrayToList( args ) );
    }

    static public Object readFunction( PushbackReader r ) throws Exception
    {
//        System.out.println( "attempting to read function" );
        skipWhitespace( r );
        readKeyword( "function", r );
        skipWhitespace( r );
        Object name = readName( r );
        skipWhitespace( r );
        Object[] params = readParams( r );
        skipWhitespace( r );
        ISeq block = readBlock( r );

//        System.out.println( "done reading function!" );

        return RT.listStar( Symbol.intern( "defn" ), name, RT.vector( params ), block );
    }

    static public ISeq readBlock( PushbackReader r ) throws Exception
    {
//        System.out.println( "attempting to read block" );
        skipWhitespace( r );
        readKeyword( "{", r );
        skipWhitespace( r );
        Object[] expressions = readExpressions( r, "}" );
//        System.out.println( "done reading block" );
        
        return RT.listStar( Symbol.intern( "do" ), RT.arrayToList( expressions ) );
//        return RT.arrayToList( expressions );
    }

    static public Object readInvocation( PushbackReader r ) throws Exception
    {
//        System.out.println( "attempting to read invocation" );
        skipWhitespace( r );
        Object name1 = readName( r );
        Object name2 = null;
        skipWhitespace( r );
        if ( readKeyword( ".", r ) )
        {
            skipWhitespace( r );
            name2 = "." + readName( r );
        }
        skipWhitespace( r );
        if ( peekFor( "(", r ) )
        {
            readKeyword( "(", r );
            Object[] args = readExpressions( r, ")" );
//            System.out.println( "done reading invocation" );
            if ( name2 == null )
            {
                return RT.listStar( name1, RT.arrayToList( args ) );
            }
            else
            {
                return RT.listStar( name2, name1, RT.arrayToList( args ) );
            }
        }
        else
        {
            return LispReader.interpretToken( ( ( Symbol ) name1 ).getName() );
        }
    }

    static public IPersistentVector getVector( PushbackReader r ) throws Exception
    {
        readKeyword( "#[", r );
        skipWhitespace( r );
        Object[] exprs = readExpressions( r, "]" );
        return RT.vector( exprs );
    }

    static public ISeq getList( PushbackReader r ) throws Exception
    {
        readKeyword( "#(", r );
        skipWhitespace( r );
        Object[] exprs = readExpressions( r, ")" );

        return RT.listStar( Compiler.QUOTE, RT.list( RT.arrayToList( exprs ) ) );
    }

    static public IPersistentSet getSet( PushbackReader r ) throws Exception
    {
        readKeyword( "#<", r );
        skipWhitespace( r );
        Object[] exprs = readExpressions( r, ">" );

        return RT.set( exprs );
    }

    static public IPersistentMap getMap( PushbackReader r ) throws Exception
    {
        readKeyword( "#{", r );
        skipWhitespace( r );
        Object[] exprs = readExpressions( r, "}" );

        if ( exprs.length % 2 != 0 )
        {
            throw new RuntimeException( "Maps require a value for every key." );
        }

        return RT.map( exprs );
    }
}
