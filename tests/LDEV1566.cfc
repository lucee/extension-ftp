component extends="org.lucee.cfml.test.LuceeTestCase" labels="ftp" {

	function run( testResults, testBox ) {
		describe( "LDEV-1566 cfftp returntype", function() {

			it( "an invalid returntype is rejected before any connection is made", function() {
				expect( function() {
					ftp action="listDir" name="local.res" server="localhost" username="x" password="x" directory="/" returntype="xml";
				}).toThrow( type="application", regex="returntype" );
			});

			it( "returntype=array and struct are accepted attribute values", function() {
				// connection fails (nothing listens on the port), the error must not be about the attributes
				for ( var rt in [ "array", "struct" ] ) {
					var msg = "";
					try {
						ftp action="listDir" name="local.res" server="localhost" port="1" username="x" password="x" directory="/" returntype=rt columnKey="name" timeout=2;
					}
					catch ( any e ) {
						msg = e.message;
					}
					expect( msg ).notToInclude( "returntype" );
					expect( msg ).notToInclude( "columnKey" );
				}
			});
		});
	}
}
