// import ....


commonOptions = <>
		<var id="highmem" value={getEnv('memory.highvalue', '2G')} />
		<var id="blaslibname" value={getEnv("blas.lib.name", "blas")} />
		<var id="java" value={getEnv('sun.java.cmd', java)} />
		<var id="jarfile" value={getEnv('net.bpiwowar.renaissance.jar')} />
	</>;
	
// ---- TO REMOVE LATTER - NOT PART OF THE LIBRARY ---- 
	
// Wikipedia collection
addData(
	<data urn="inex.wikipedia.2008">
		<var id="path" value="/home/inex/.../wiik" />
	</data>
);

// --- Experiment

for each(var weighting in ["tf", "tf-idf"])
for each(var sampling in [2000])
for each(var maxrank in [100])
{
	kd = getTask("uk.ac.gla.dcs.renaissance.iqir.build-keyword-density");
    kd.setParameter({"weighting": weighing, "sampling": sampling, "max-rank": maxrank});
	for each (var tdweight in ["none", "tf-idf"]) {
    }
}

// --- END ---




// ---	
// --- Build the keyword representation
// ---

addTask(function() {
    
    // --- Identifier and description
    
    this.id = "uk.ac.gla.dcs.renaissance.iqir.build-keyword-density";
    
    this.description = <>
        <p>Constructs the keyword representation for a set of keywords</p>
        <p>The input should be a document cache, </p>
        
    <>;
    

    // --- Options (can be changed without influencing the nature of the result, e.g. library name, etc.)
   	this.getOptions = function() {
		return <>{commonOptions}	
				<var id="dbname" default="keywords"/>
				<var id="dbdir" default={workdir + "/keywords"} />
			   </>;
	};
	
    // --- Parameters influence the outcome of the task
	this.getParameters = function() {
		return 	<>
			<data id="documents" ref="uk.ac.gla.dcs.renaissance.iqir.documents"/>
			<data id="index" ref="mg4j.index" format="3.0"/>
            <data id="term-processor" ref="uk.ac.gla.dcs.renaissance.iqir.documents"/>
        </>;
	};

    // --- Initialisation of the object
	// Should check everything is fine in the parameters
	// and initialise the object
	this.init = function() {
	    defined(params.indocs.(@id = "net.bpiwowar.documents.xmlpos"))
	    defined(params.term_processor.(@id = "net.bpiwowar.documents.xmlpos"))    
	};
	
    
	// --- Says what are the output (given the current parameters)
	this.outputs = function() {
	    return <>
	    	<output id="outdocs" type="uk.ac.gla.dcs.renaissance.iqir.documents" view="cache" version="1.0" format="1.0">
				<var id="dbdir">{workdir}/db</var>
				<var id="dbname">documents</var>
			</output>
		</>;
	};

    // --- run is called when the manager need to execute the task
	this.run = function() {
			command = [ java, "-Xmx1G", "-jar", jarfile, "keyword-builder", "--dbname", dbname, "--dbdir", getPath(workdir, db), "--dir",
			getpath(workdir, "index"), "--basename", "index", "--field", "text",
			"--term-processor", "$TERMPROCESSOR", "transform", "--sequence",
			"$OUTDIR/mg4j.seq", "--unit-boundaries", "p", "--destroy-old-database", "--nb-reader-threads", nbthreads ];
	        run(command);
	};

});


// ---	
// --- Creates a document cache
// ---

addTask(function() {
    this.id = "uk.ac.gla.dcs.renaissance.iqir.document-cache";
    
    this.description = <>
    	<p>Builds a cache for documents</p>
    </>;
    
    this.dbname = "dbname";
	
	this.getOptions = function() {
		return <>{commonOptions}	
				<var id="dbname" default="keywords"/>
				<var id="dbdir" default={workdir + "/keywords"} />
			   </>;
	};
	
	this.getParameters = function() {
		return 	<>
			<data id="indocs" ref="uk.ac.gla.dcs.renaissance.iqir.documents"/>
			<data id="term-processor" ref="uk.ac.gla.dcs.renaissance.iqir.documents"/>
			</>;
	};
	
	// Should check everything is fine in the parameters
	// and initialise the object
	this.init = function() {
	    defined(params.indocs.(@id = "net.bpiwowar.documents.xmlpos"))
	    defined(params.term_processor.(@id = "net.bpiwowar.documents.xmlpos"))    
	};
	
	// Says what are the output (given the current parameters)
	this.outputs = function() {
	    return <>
	    	<output id="outdocs" type="uk.ac.gla.dcs.renaissance.iqir.documents" view="cache" version="1.0" format="1.0">
				<var id="dbdir">{workdir}/db</var>
				<var id="dbname">documents</var>
			</output>
		</>;
	};
	
	// run is called when the manager need to execute the task
	this.run = function() {
			command = [ java, "-Xmx1G", "-jar", jarfile, "keyword-builder", "--dbname", dbname, "--dbdir", getPath(workdir, db), "--dir",
			getpath(workdir, "index"), "--basename", "index", "--field", "text",
			"--term-processor", "$TERMPROCESSOR", "transform", "--sequence",
			"$OUTDIR/mg4j.seq", "--unit-boundaries", "p", "--destroy-old-database", "--nb-reader-threads", nbthreads ];
	        run(command);
	};
	
});