// --- Include repositories

var irc = include_repository(".../etc/irc.js");

var mg4j = include_repository(".../main.js").get("namespace");

// --- Run the experiments

logging.info("Index and perform IR ad-hoc experiments on TREC 1992 & 1993");

tasks.mg4j::adhoc.run({
    // Defines the collection
    "collection.id": ["trec/1992/adhoc", "trec/1993/adhoc"],
    
    // Defines the output directory
    "outdir": "index",
    
    // defines the model to use
    "model": "mg4j:bm25",
    "model.k1": 5
});