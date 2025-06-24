;; src/rubberbuf/ebnf.cljc is auto-generated and resources/ebnf/enbf.cljc.template for auto-gen.
;; If resources/ebnf/*.ebnf is updated, run `clj -X:write-ebnf-cljc` to re-generate.

(ns rubberbuf.ebnf)

(def proto2-ebnf "
proto = syntax { import | package | option | message | enum | extend | service | <emptyStatement> };

octalDigit3  = #'[0-7][0-7][0-7]';
hexDigit     = #'[0-9a-fA-F]';
hexDigit2    = #'[0-9a-fA-F][0-9a-fA-F]';

ident = #'[a-zA-Z_][a-zA-Z0-9_]*';
fullIdent = ident { '.' ident };
messageName = ident;
enumName = ident;
fieldName = ident;
oneofName = ident;
mapName = ident;
serviceName = ident;
rpcName = ident;
(* messageType = [ '.' ] { ident '.' } messageName; *)
messageType = #'^((\\.[A-Za-z_][A-Za-z0-9_]*)|[A-Za-z_][A-Za-z0-9_]*)(\\.[A-Za-z_][A-Za-z0-9_]*)*';

(* proto2 only *)
streamName = ident;
groupName = #'[A-Z_][a-zA-Z0-9_]*';
group = label <'group'> groupName <'='> fieldNumber messageBody;
stream = <'stream'> streamName <'('> messageType <','> messageType <')'> (( '{' { option | <emptyStatement> } '}') | <';'> );
extend = <'extend'> messageType <'{'> {field | group | <emptyStatement>} <'}'>;
extensions = <'extensions'> ranges [ <'['> fieldOptions <']'> ] <';'>;

(* proto2 only: 'required' *)
label = 'required' | 'optional' | 'repeated';

intLit     = decimalLit | octalLit | hexLit;
sintLit    = ( [ '-' | '+' ] intLit );
decimalLit = #'[1-9][0-9]*';
octalLit   = #'0[0-7]*';
hexLit     = #'(0x|0X)[0-9a-fA-F][0-9a-fA-F]*';

floatLit = ( decimals '.' [ decimals ] [ exponent ] | decimals exponent | '.'decimals [ exponent ] ) | 'inf' | 'nan';
decimals  = #'[0-9]+';
exponent  = ( 'e' | 'E' ) [ '+' | '-' ] decimals;

boolLit = 'true' | 'false';

strLit = ( <\"'\"> { charValue } <\"'\"> ) |  ( <'\"'> { charValue } <'\"'> );
charValue = hexEscape | octEscape | charEscape |  #'[^\\x00\\n\\\\]';
hexEscape = <('\\\\x' | '\\\\X')> hexDigit2;
octEscape = <'\\\\'> octalDigit3;
charEscape = <'\\\\'> ( 'a' | 'b' | 'f' | 'n' | 'r' | 't' | 'v' | '\\\\' | \"'\" | '\"' );

emptyStatement = <(#'[\\s\\n\\r;]*')>;

constant = fullIdent | ( [ '-' | '+' ] intLit ) | ( [ '-' | '+' ] floatLit ) | strLit | boolLit;

singleQuote = \"'\";
doubleQuote = '\"';
syntax = <'syntax'> <'='> ( <singleQuote> 'proto2' <singleQuote> |
                            <doubleQuote> 'proto2' <doubleQuote> ) <';'>;

import = <'import'> [ 'weak' | 'public' ] strLit <';'>;

package = <'package'> fullIdent <';'>;

option = <'option'> optionName  <'='> tf_Constant <';'>;
optionName = ( ident | '(' fullIdent ')' ) { '.' ident };

priType = 'double' | 'float' | 'int32' | 'int64' | 'uint32' | 'uint64'
      | 'sint32' | 'sint64' | 'fixed32' | 'fixed64' | 'sfixed32' | 'sfixed64'
      | 'bool' | 'string' | 'bytes';
type = messageType | priType;

field = label type fieldName <'='> fieldNumber [ <'['> fieldOptions <']'> ] <';'>;

fieldNumber = intLit;
fieldOptions = fieldOption { <','>  fieldOption };
fieldOption = optionName <'='> tf_Constant;

oneof = <'oneof'> oneofName <'{'> { oneofField | <emptyStatement> } <'}'>;
oneofField = type fieldName <'='> fieldNumber [ <'['> fieldOptions <']'> ] <';'>;

mapField = <'map'> <'<'> keyType <','> type <'>'> mapName <'='> fieldNumber [ <'['> fieldOptions <']'> ] <';'>;
keyType = 'int32' | 'int64' | 'uint32' | 'uint64' | 'sint32' | 'sint64' |
          'fixed32' | 'fixed64' | 'sfixed32' | 'sfixed64' | 'bool' | 'string';

<ranges> = range { <','> range };
range =  intLit [ <'to'> ( intLit | 'max' ) ];

reserved-ranges = <'reserved'> ranges <';'>;
reserved-names = <'reserved'> strFieldNames <';'>;
<reserved> = reserved-ranges | reserved-names;
<strFieldNames> = strFieldName { <','> strFieldName };
<strFieldName> = <'\"'> fieldName <'\"'> | <\"'\"> fieldName <\"'\">;

enum = <'enum'> enumName enumBody;
<enumBody> = <'{'> { option | enumField | reserved | <emptyStatement> } <'}'>;
enumField = ident <'='> sintLit [ <'['> fieldOptions <']'> ]<';'>;

message = <'message'> messageName messageBody;
(* proto2 only: extensions, group *)
<messageBody> = <'{'> { field | enum | message | extend | extensions | group |
option | oneof | mapField | reserved | <emptyStatement> } <'}'>;


(* proto2 only: stream *)
service = <'service'> serviceName <'{'> { option | rpc | stream | <emptyStatement> } <'}'>;

returnType = messageType;
rpcLabel = ['stream'];
rpc = <'rpc'> rpcName <'('> rpcLabel messageType <')'> <'returns'> <'('> 
              rpcLabel returnType <')'> (( <'{'> { option | <emptyStatement> } <'}'> ) | <';'> );

(* tf_Constant is defined in textformat.ebnf *)
")

(def proto3-ebnf "
proto = syntax { import | package | option | message | enum | extend | service | <emptyStatement> };

octalDigit3  = #'[0-7][0-7][0-7]';
hexDigit     = #'[0-9a-fA-F]';
hexDigit2    = #'[0-9a-fA-F][0-9a-fA-F]';

ident = #'[a-zA-Z_][a-zA-Z0-9_]*';
fullIdent = ident { '.' ident };
messageName = ident;
enumName = ident;
fieldName = ident;
oneofName = ident;
mapName = ident;
serviceName = ident;
rpcName = ident;
(* messageType = [ '.' ] { ident '.' } messageName; *)
messageType = #'^((\\.[A-Za-z_][A-Za-z0-9_]*)|[A-Za-z_][A-Za-z0-9_]*)(\\.[A-Za-z_][A-Za-z0-9_]*)*';

extend = <'extend'> messageType <'{'> {field | <emptyStatement>} <'}'>;

(* proto2 only: 'required' *)
label = [ 'optional' | 'repeated' ];

intLit     = decimalLit | octalLit | hexLit;
sintLit    = ( [ '-' | '+' ] intLit );
decimalLit = #'[1-9][0-9]*';
octalLit   = #'0[0-7]*';
hexLit     = #'(0x|0X)[0-9a-fA-F][0-9a-fA-F]*';

<floatLit> = ( decimals '.' [ decimals ] [ exponent ] | decimals exponent | '.'decimals [ exponent ] ) | 'inf' | 'nan';

<decimals>  = #'[0-9]+';
<exponent>  = ( 'e' | 'E' ) [ '+' | '-' ] decimals;

boolLit = 'true' | 'false';

strLit = ( <'\"'> { charValue } <'\"'> ) |  ( <\"'\"> { charValue } <\"'\"> );
charValue = hexEscape | octEscape | charEscape |  #'[^\\x00\\n\\\\]';
hexEscape = <('\\\\x' | '\\\\X')> hexDigit2;
octEscape = <'\\\\'> octalDigit3;
charEscape = <'\\\\'> ( 'a' | 'b' | 'f' | 'n' | 'r' | 't' | 'v' | '\\\\' | '\"' | \"'\" );

emptyStatement = <(#'[\\s\\n\\r;]*')>;

signedFloatLit = [ '-' | '+' ] floatLit;
signedIntLit = [ '-' | '+' ] intLit;
<constant> = fullIdent | signedIntLit | signedFloatLit | strLit | boolLit;

singleQuote = \"'\";
doubleQuote = '\"';
syntax = <'syntax'> <'='> ( <singleQuote> 'proto3' <singleQuote> |
                            <doubleQuote> 'proto3' <doubleQuote> ) <';'>;

import = <'import'> [ 'weak' | 'public' ] strLit <';'>;

package = <'package'> fullIdent <';'>;

option = <'option'> optionName  <'='> tf_Constant <';'>;
optionName = ( ident | '(' fullIdent ')' ) { '.' ident };

priType = 'double' | 'float' | 'int32' | 'int64' | 'uint32' | 'uint64'
      | 'sint32' | 'sint64' | 'fixed32' | 'fixed64' | 'sfixed32' | 'sfixed64'
      | 'bool' | 'string' | 'bytes';
type = messageType | priType;

field = label type fieldName <'='> fieldNumber [ <'['> fieldOptions <']'> ] <';'>;

fieldNumber = intLit;
fieldOptions = fieldOption { <','>  fieldOption };
fieldOption = optionName <'='> tf_Constant;

oneof = <'oneof'> oneofName <'{'> { oneofField | <emptyStatement> } <'}'>;
oneofField = type fieldName <'='> fieldNumber [ <'['> fieldOptions <']'> ] <';'>;

mapField = <'map'> <'<'> keyType <','> type <'>'> mapName <'='> fieldNumber [ <'['> fieldOptions <']'> ] <';'>;
keyType = 'int32' | 'int64' | 'uint32' | 'uint64' | 'sint32' | 'sint64' |
          'fixed32' | 'fixed64' | 'sfixed32' | 'sfixed64' | 'bool' | 'string';

<ranges> = range { <','> range };
range =  intLit [ <'to'> ( intLit | 'max' ) ];

reserved-ranges = <'reserved'> ranges <';'>;
reserved-names = <'reserved'> strFieldNames <';'>;
<reserved> = reserved-ranges | reserved-names;
<strFieldNames> = strFieldName { <','> strFieldName };
<strFieldName> = <'\"'> fieldName <'\"'> | <\"'\"> fieldName <\"'\">;

enum = <'enum'> enumName enumBody;
<enumBody> = <'{'> { option | enumField | reserved | <emptyStatement> } <'}'>;
enumField = ident <'='> sintLit [ <'['> fieldOptions <']'> ]<';'>;

message = <'message'> messageName messageBody;
<messageBody> = <'{'> { field | enum | message | extend | option | oneof | mapField |
                        reserved | <emptyStatement> } <'}'>;


(* proto2 only: stream *)
service = <'service'> serviceName <'{'> { option | rpc | <emptyStatement> } <'}'>;

returnType = messageType;
rpcLabel = ['stream'];
rpc = <'rpc'> rpcName <'('> rpcLabel messageType <')'> <'returns'> <'('> 
              rpcLabel returnType <')'> (( <'{'> { option | <emptyStatement> } <'}'> ) | <';'> );

(* tf_Constant is defined in textformat.ebnf *)
")

(def protoE-ebnf "
proto = edition { import | package | option | message | enum | extend | service | <emptyStatement> };

octalDigit3  = #'[0-7][0-7][0-7]';
hexDigit     = #'[0-9a-fA-F]';
hexDigit2    = #'[0-9a-fA-F][0-9a-fA-F]';

ident = #'[a-zA-Z_][a-zA-Z0-9_]*';
fullIdent = ident { '.' ident };
messageName = ident;
enumName = ident;
fieldName = ident;
oneofName = ident;
mapName = ident;
serviceName = ident;
rpcName = ident;
(* messageType = [ '.' ] { ident '.' } messageName; *)
messageType = #'^((\\.[A-Za-z_][A-Za-z0-9_]*)|[A-Za-z_][A-Za-z0-9_]*)(\\.[A-Za-z_][A-Za-z0-9_]*)*';

extend = <'extend'> messageType <'{'> {field | <emptyStatement>} <'}'>;

(* proto2 only: 'required' *)
(* proto2|3 only: 'optional' *)
label = [ 'repeated' ];

intLit     = decimalLit | octalLit | hexLit;
sintLit    = ( [ '-' | '+' ] intLit );
decimalLit = #'[1-9][0-9]*';
octalLit   = #'0[0-7]*';
hexLit     = #'(0x|0X)[0-9a-fA-F][0-9a-fA-F]*';

<floatLit> = ( decimals '.' [ decimals ] [ exponent ] | decimals exponent | '.'decimals [ exponent ] ) | 'inf' | 'nan';

<decimals>  = #'[0-9]+';
<exponent>  = ( 'e' | 'E' ) [ '+' | '-' ] decimals;

boolLit = 'true' | 'false';

strLit = ( <'\"'> { charValue } <'\"'> ) |  ( <\"'\"> { charValue } <\"'\"> );
charValue = hexEscape | octEscape | charEscape |  #'[^\\x00\\n\\\\]';
hexEscape = <('\\\\x' | '\\\\X')> hexDigit2;
octEscape = <'\\\\'> octalDigit3;
charEscape = <'\\\\'> ( 'a' | 'b' | 'f' | 'n' | 'r' | 't' | 'v' | '\\\\' | '\"' | \"'\" );

emptyStatement = <(#'[\\s\\n\\r;]*')>;

signedFloatLit = [ '-' | '+' ] floatLit;
signedIntLit = [ '-' | '+' ] intLit;
<constant> = fullIdent | signedIntLit | signedFloatLit | strLit | boolLit;

singleQuote = \"'\";
doubleQuote = '\"';
edition = <'edition'> <'='> ( <singleQuote> yyyy <singleQuote> |
                              <doubleQuote> yyyy <doubleQuote> ) <';'>;
<yyyy> = #'20[2-9][0-9]'; (* smallest is 2023 *)

import = <'import'> [ 'weak' | 'public' ] strLit <';'>;

package = <'package'> fullIdent <';'>;

option = <'option'> optionName  <'='> tf_Constant <';'>;
optionName = ( ident | '(' fullIdent ')' ) { '.' ident };

priType = 'double' | 'float' | 'int32' | 'int64' | 'uint32' | 'uint64'
      | 'sint32' | 'sint64' | 'fixed32' | 'fixed64' | 'sfixed32' | 'sfixed64'
      | 'bool' | 'string' | 'bytes';
type = messageType | priType;

field = label type fieldName <'='> fieldNumber [ <'['> fieldOptions <']'> ] <';'>;

fieldNumber = intLit;
fieldOptions = fieldOption { <','>  fieldOption };
fieldOption = optionName <'='> tf_Constant;

oneof = <'oneof'> oneofName <'{'> { oneofField | <emptyStatement> } <'}'>;
oneofField = type fieldName <'='> fieldNumber [ <'['> fieldOptions <']'> ] <';'>;

mapField = <'map'> <'<'> keyType <','> type <'>'> mapName <'='> fieldNumber [ <'['> fieldOptions <']'> ] <';'>;
keyType = 'int32' | 'int64' | 'uint32' | 'uint64' | 'sint32' | 'sint64' |
          'fixed32' | 'fixed64' | 'sfixed32' | 'sfixed64' | 'bool' | 'string';

<ranges> = range { <','> range };
range =  intLit [ <'to'> ( intLit | 'max' ) ];

reserved-ranges = <'reserved'> ranges <';'>;
reserved-names = <'reserved'> strFieldNames <';'>;
<reserved> = reserved-ranges | reserved-names;
<strFieldNames> = strFieldName { <','> strFieldName };
<strFieldName> = fieldName;

enum = <'enum'> enumName enumBody;
<enumBody> = <'{'> { option | enumField | reserved | <emptyStatement> } <'}'>;
enumField = ident <'='> sintLit [ <'['> fieldOptions <']'> ]<';'>;

message = <'message'> messageName messageBody;
<messageBody> = <'{'> { field | enum | message | extend | option | oneof | mapField |
                        reserved | <emptyStatement> } <'}'>;


(* proto2 only: stream *)
service = <'service'> serviceName <'{'> { option | rpc | <emptyStatement> } <'}'>;

returnType = messageType;
rpcLabel = ['stream'];
rpc = <'rpc'> rpcName <'('> rpcLabel messageType <')'> <'returns'> <'('> 
              rpcLabel returnType <')'> (( <'{'> { option | <emptyStatement> } <'}'> ) | <';'> );

(* tf_Constant is defined in textformat.ebnf *)
")

(def proto-version-ebnf "proto = syntax | edition;

syntax = <'syntax'> <'='> <quote> version <quote> <';'> {< #'.*' >};
version = 'proto2' | 'proto3';

edition = <'edition'> <'='> <quote> yyyy <quote> <';'> {< #'.*' >};
yyyy =  #'20[2-9][0-9]'; (* smallest is 2023 *)

quote = \"'\" | '\"';
")

(def textformat-ebnf "
<tf_char>    = #'[^\\x00\\n\\\\]';
tf_newline   = #'\\n';

(* letter = 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G' | 'H' | 'I' | 'J' | 'K' | 'L' | 'M'
       | 'N' | 'O' | 'P' | 'Q' | 'R' | 'S' | 'T' | 'U' | 'V' | 'W' | 'X' | 'Y' | 'Z'
       | 'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g' | 'h' | 'i' | 'j' | 'k' | 'l' | 'm'
       | 'n' | 'o' | 'p' | 'q' | 'r' | 's' | 't' | 'u' | 'v' | 'w' | 'x' | 'y' | 'z'
       | '_' ; *)

<tf_oct> = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' ;
<tf_dec> = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ;
<tf_hex> = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
         | 'A' | 'B' | 'C' | 'D' | 'E' | 'F'
         | 'a' | 'b' | 'c' | 'd' | 'e' | 'f' ;

tf_COMMENT    = '#', { (!tf_newline) tf_char }, [ tf_newline ] ;
tf_WHITESPACE = #'[ \\n\\r\\t\\v]*';

(* IDENT = letter, { letter | dec } ; *)
<tf_IDENT> = #'[a-zA-Z_][a-zA-Z0-9_]*';

<tf_dec_lit>   = #'[1-9][0-9]*';
<tf_decimals>  = #'[0-9]+';

<tf_float_lit> = '.', tf_decimals, [ tf_exp ]
               | tf_dec_lit, '.', tf_decimals, [ tf_exp ]
               | '0.', tf_decimals, [tf_exp]
               | tf_dec_lit, tf_exp ;
<tf_exp>       = ( 'E' | 'e' ), [ '+' | '-' ], tf_dec_lit ;

<tf_DEC_INT>   = ( tf_dec_lit | tf_decimals );
<tf_OCT_INT>   = '0', tf_oct, { tf_oct } ;
<tf_HEX_INT>   = '0', ( 'X' | 'x' ), tf_hex, { tf_hex } ;
<tf_FLOAT>     = tf_float_lit, [ 'F' | 'f' ]
               | tf_dec_lit,   ( 'F' | 'f' ) ;

<tf_STRING> = tf_single_string | tf_double_string ;
(*                      | any char    | escape           | UTF-8 byte   | UTF-8 byte          | Unicode code pt   |Unicode code pt       | Unicode code pt btw  |*)
(*                      | excluding   |                  | in octal     | in hexadecimal      | up to 0xffff      |up to 0xfffff         | 0x100000 & 0x10ffff  |*)
<tf_double_string> = #'\"([^\\\"\\x00\\n\\\\]|(\\\\[abfnrtv?\\\"\\\\])|(\\\\[0-7]{1,3})|(\\\\x[0-9a-fA-F]{1,2})|(\\\\u[0-9a-fA-F]{4})|(\\\\U000[0-9a-fA-F]{5})|(\\\\U010[0-9a-fA-F]{4}))*\"';
<tf_single_string> = #\"'([^\\'\\x00\\n\\\\]|(\\\\[abfnrtv?\\'\\\\])|(\\\\[0-7]{1,3})|(\\\\x[0-9a-fA-F]{1,2})|(\\\\u[0-9a-fA-F]{4})|(\\\\U000[0-9a-fA-F]{5})|(\\\\U010[0-9a-fA-F]{4}))*'\";

tf_escape = '\\\\a'                        (* ASCII #7  (bell)                 *)
          | '\\\\b'                        (* ASCII #8  (backspace)            *)
          | '\\\\f'                        (* ASCII #12 (form feed)            *)
          | '\\\\n'                        (* ASCII #10 (line feed)            *)
          | '\\\\r'                        (* ASCII #13 (carriage return)      *)
          | '\\\\t'                        (* ASCII #9  (horizontal tab)       *)
          | '\\\\v'                        (* ASCII #11 (vertical tab)         *)
          | '\\\\?'                        (* ASCII #63 (question mark)        *)
          | '\\\\\\\\'                       (* ASCII #92 (backslash)            *)
          | \"'\"                          (* ASCII #39 (apostrophe)           *)
          | '\\\\\\''                       (* ASCII #34 (quote)                *)
          | '\\\\', tf_oct, [ tf_oct, [ tf_oct ] ]  (* UTF-8 byte in octal              *)
          | '\\\\x', tf_hex, [ tf_hex ]          (* UTF-8 byte in hexadecimal        *)
          | '\\\\u', tf_hex, tf_hex, tf_hex, tf_hex    (* Unicode code point up to 0xffff  *)
          | '\\\\U000',
            tf_hex, tf_hex, tf_hex, tf_hex, tf_hex      (* Unicode code point up to 0xfffff *)
          | '\\\\U0010',
            tf_hex, tf_hex, tf_hex, tf_hex           (* Unicode code point between 0x100000 and 0x10ffff *)
          ;

<tf_Constant> = tf_MessageList | tf_ScalarList | tf_MessageValue | tf_ScalarValue;

tf_Message = { tf_Field } ;

tf_String             = tf_STRING, { tf_STRING } ;
tf_Float              = [ '-' ], tf_FLOAT ;
tf_Identifier         = tf_IDENT ;
tf_SignedIdentifier   = '-', tf_IDENT ;   (* For example, '-inf' *)
tf_DecSignedInteger   = '-', tf_DEC_INT ;
tf_OctSignedInteger   = '-', tf_OCT_INT ;
tf_HexSignedInteger   = '-', tf_HEX_INT ;
tf_DecUnsignedInteger = tf_DEC_INT ;
tf_OctUnsignedInteger = tf_OCT_INT ;
tf_HexUnsignedInteger = tf_HEX_INT ;

tf_FieldName       = tf_ExtensionName | tf_AnyName | tf_IDENT ;
<tf_ExtensionName> = '[', tf_TypeName, ']' ;
<tf_AnyName>       = '[', tf_Domain, '/', tf_TypeName, ']' ;
<tf_TypeName>      = tf_IDENT, { '.', tf_IDENT } ;
<tf_Domain>        = tf_IDENT, { '.', tf_IDENT } ;

tf_Field          = tf_ScalarField | tf_MessageField ;
tf_MessageField   = tf_FieldName, [ <':'> ], ( tf_MessageValue | tf_MessageList ) [ <';'> | <','> ];
tf_ScalarField    = tf_FieldName, <':'>,     ( tf_ScalarValue  | tf_ScalarList  ) [ <';'> | <','> ];
tf_MessageList    = <'['>, [ tf_MessageValue, { <','>, tf_MessageValue } ], <']'> ;
tf_ScalarList     = <'['>, [ tf_ScalarValue,  { <','>, tf_ScalarValue  } ], <']'> ;
<tf_MessageValue> = <'{'>, tf_Message, <'}'> | <'<'>, tf_Message, <'>'> ;
<tf_ScalarValue>  = tf_String
                  | tf_Float
                  | tf_Identifier
                  | tf_SignedIdentifier
                  | tf_DecSignedInteger
                  | tf_OctSignedInteger
                  | tf_HexSignedInteger
                  | tf_DecUnsignedInteger
                  | tf_OctUnsignedInteger
                  | tf_HexUnsignedInteger ;

tf_signedInteger   = tf_DecSignedInteger | tf_OctSignedInteger | tf_HexSignedInteger ;
tf_unsignedInteger = tf_DecUnsignedInteger | tf_OctUnsignedInteger | tf_HexUnsignedInteger ;
tf_integer         = tf_signedInteger | tf_unsignedInteger ;
")
