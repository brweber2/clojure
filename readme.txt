This is a FORK of Clojure and not the official version of Clojure!  More
specifically this is a fork of clojure 1.2.0-master-SNAPSHOT. This is NOT
a stable release of Clojure, it is highly experimental.  Use at your own
risk.
  
WHY A FORK?

This fork provides an alternate reader in addition to the LispReader.  The
goal is to provide a syntax that is more familiar to non-Lisp programmers.
To use the LispReader name your file with the traditional .clj extension.
To use the alternate syntax reader name your source file with the .bbw 
extension.  The advantage to having an alternate reader is that all the
reader has to do is parse the source and return Clojure's existing data
structures.  The compiler and RT have remained virtually unchanged.

WHAT DOES THE GRAMMAR LOOK LIKE?

It is still very much a moving target, documentation will be added as it
stabilizes.  You can view a working sample in the 'how do I try it out?' 
section below.

HOW DO I TRY IT OUT?

Save the following content to a file named "source.bbw"
==========================================================================
function hi [ java.lang.String name ] {
  println(name)
  println(name)
}

hi("holy smokes!")
==========================================================================

To Build: ant
To Run: java -cp clojure-1.2.0-master-SNAPSHOT.jar source.bbw

You will notice a lot of garbage printing to standard out.  You can safely 
ignore that for now, it will be removed when the grammar stabilizes somewhat.
You should see the string "holy smokes!" print twice near the end of the 
output.

List of Modified Files:

readme.txt
src/jvm/clojure/lang/LineNumberingPushbackReader.java
src/jvm/clojure/lang/LispReader.java
src/jvm/clojure/lang/Compiler.java

List of New Files:

src/jvm/clojure/lang/AlternateReader.java

--------------------------------------------------------------------------

 *   Clojure
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.

Docs: http://clojure.org
Feedback: http://groups.google.com/group/clojure

To Run: java -cp clojure.jar clojure.main
To Build: ant

--------------------------------------------------------------------------
This program uses the ASM bytecode engineering library which is distributed
with the following notice:

Copyright (c) 2000-2005 INRIA, France Telecom
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holders nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
THE POSSIBILITY OF SUCH DAMAGE.