setwd(normalizePath(dirname(R.utils::commandArgs(asValues=TRUE)$"f")))
source('../h2o-runit.R')

test.headers <- function(conn) {

  headers <- h2o.importFile(conn, locate("smalldata/airlines/allyears2k_headers_only.csv"), destination_frame = "airlines_headers")
  hex <- h2o.importFile(conn, locate("smalldata/airlines/allyears2k.zip"), col.names=headers, destination_frame = "airlines")
  print(names(headers))
  print(names(hex))
  checkIdentical(names(headers), names(hex))

  testEnd()
}

doTest("Import a dataset with a header H2OParsedData Object", test.headers)
