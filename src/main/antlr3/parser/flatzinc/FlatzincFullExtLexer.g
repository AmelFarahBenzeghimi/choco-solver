lexer grammar FlatzincFullExtLexer;

@lexer::header {
/*
 * Copyright (c) 1999-2012, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package parser.flatzinc;
}


/*********************************************
 * KEYWORDS
 **********************************************/

BOOL:'bool';
TRUE:'true';
FALSE:'false';
INT:'int';
FLOAT:'float';
SET :'set';
OF :'of';
ARRAY :'array';
VAR :'var';
PAR :'par';
PREDICATE :'predicate';
CONSTRAINT :  'constraint';
SOLVE :'solve';
SATISFY :'satisfy';
MINIMIZE :'minimize';
MAXIMIZE :'maximize';

DD:'..';
DO:'.';
LB:'{';
RB:'}';
CM:',';
LS:'[';
RS :']';
EQ:'=';
PL:'+';
MN:'-';
SC:';';
CL:':';
DC:'::';
LP:'(';
RP:')';


/*********************
 *  EXTENSION
 *********************/
AS:'as';
EACH:'each';


QUEUE:'queue';
LIST:'list';
HEAP:'heap';

ONE:'one';
WONE:'wone';
FOR:'for';
WFOR:'wfor';
ORDERBY:'order by';
AND:'&&';
OR:'||';
NOT:'!';
IN:'in';
REV:'rev';
OEQ:'==';
ONQ:'!=';
OLT:'<';
OGT:'>';
OLQ:'<=';
OGQ:'>=';
KEY:'key';

/*********************
 *  ATTRIBUTES
 *********************/
CSTR:'cstr';
PROP:'prop';
VNAME:'var.name';
VCARD:'var.cardinality';
CNAME:'cstr.name';
CARITY:'cstr.arity';
PPRIO:'prop.priority';
PARITY:'prop.arity';
PPRIOD:'prop.prioDyn';
ANY:'any';
MIN:'min';
MAX:'max';
SUM:'sum';
SIZE:'size';


/*********************************************
 * EXTRA
 **********************************************/

APAR    :   '###_P###';
ARRPAR  :   '###AP###';
AVAR    :   '###_V###';
ARRVAR  :   '###AV###';
INDEX   :   '###ID###';
EXPR    :   '###EX###';
ANNOTATIONS:'###AS###';

STRUC1:'###ST1##';
STRUC2:'###ST2##';
STREG:'###SR###';
MANY1:'###M1###';
MANY2:'###M2###';
MANY3:'###M3###';
MANY4:'###M4###';
CA1:'###CA1###';
CA2:'###CA2###';
ENGINE:'###EN###';

/*********************************************
 * GENERAL
 **********************************************/


IDENTIFIER
    :   ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
    ;


COMMENT
    :   '%' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
    ;

WS  :   ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) {$channel=HIDDEN;}
    ;

/*********************************************
 * TYPES
 **********************************************/

INT_CONST
    :   ('+'|'-')? ('0'..'9')+
    ;

//FLOAT_
//    :   ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
//    |   '.' ('0'..'9')+ EXPONENT?
//    |   ('0'..'9')+ EXPONENT
//    ;

STRING
    :  '"' ( ESC_SEQ | ~('\\'|'"') )* '"'
    ;

CHAR:  '\'' ( ESC_SEQ | ~('\''|'\\') ) '\''
    ;

/*********************************************
 * FRAGMENTS
 **********************************************/


fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UNICODE_ESC
    |   OCTAL_ESC
    ;

fragment
OCTAL_ESC
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

