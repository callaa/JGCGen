<!DOCTYPE html>
#macro(navi $file $title)
#if($inputfile=="${file}.vm")
<li><span>$title</span></li>
#else
<li><a href="${file}.html">$title</a></li>
#end
#end
<html>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<head>
<title>JGCGen documentation</title>
<link href="style/jgcgen.css" type="text/css" rel="stylesheet">
</head>
<body>

<div id="sidebar">
<div id="logo">
<h1>JGCGen</h1>
<p>A G-Code generator and preprocessor</p>
</div>
<ul id="navilist">
#navi("index", "Introduction")
#navi("dir", "Directives")
#navi("macros", "Macros")
#navi("vars", "Variables")
#navi("path", "Paths")
#navi("font", "Fonts")
#navi("outline", "Outlines")
#navi("pocket", "Pockets")
#navi("image", "Images")
#navi("surfaces", "Surfaces")
#navi("samples", "Samples")
</ul>

</div>

<div id="main">
$main
</div>

</body>
</html>
