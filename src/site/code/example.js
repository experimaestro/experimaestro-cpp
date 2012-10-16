// --- Include repositories

var irc = include_repository(".../etc/irc.js");
var ns_irc = irc.get("namespace");

var mg4jext = include_repository(".../main.js");
var ns_mg4jext = mg4jext.get("namespace");

// --- Run the experiments

xpm.log("Index and perform IR ad-hoc experiments on TREC 1992 & 1993");
plan = "collection.id=trec/1992/adhoc,trec/1993/adhoc 
	* collection.restrict=trec.ap8889 * outdir=index * model={"
	+ ns_mg4jext.uri + "}bm25";
xpm.get_task(ns_mg4jext, "adhoc").run(plan);