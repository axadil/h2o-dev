#-----------------------------------------------------------------------
# Purpose:  This test shows import from s3 and hdfs from Hadoop cluster.
#-----------------------------------------------------------------------

setwd(normalizePath(dirname(R.utils::commandArgs(asValues=TRUE)$"f")))
source('../h2o-runit-hadoop.R')

ipPort <- get_args(commandArgs(trailingOnly = TRUE))
myIP   <- ipPort[[1]]
myPort <- ipPort[[2]]
hdfs_name_node <- Sys.getenv(c("NAME_NODE"))
print(hdfs_name_node)
aws_id <- Sys.getenv(c("AWS_ID"))
aws_key <- Sys.getenv(c("AWS_Key"))
print(aws_id)
print(aws_key)

library(RCurl)
library(h2o)

#----------------------------------------------------------------------

heading("BEGIN TEST")
conn <- h2o.init(ip=myIP, port=myPort, startH2O = FALSE)
s3_airlines_file <- "h2o-airlines-unpacked/allyears2k.csv"
hdfs_iris_file <- "/datasets/allyears2k_headers.zip"
s3_url <- paste0("s3n://", aws_id, ":", aws_key, "@", s3_airlines_file)
hdfs_url <- sprintf("hdfs://%s%s", hdfs_name_node, hdfs_iris_file)

airlines.hex <- h2o.importFile(conn, s3_url)
airlines_hdfs.hex <- h2o.importFile(conn, hdfs_url)
print(summary(airlines.hex))

# Set predictor and response variables
myY <- "IsDepDelayed"
myX <- c("Origin", "Dest", "Year", "UniqueCarrier", "DayOfWeek",
         "Month", "Distance", "FlightNum")

## Simple GLM - Predict Delays
data.glm <- h2o.glm(y = myY, x = myX, training_frame = airlines.hex, 
                    family = "binomial", standardize=T, lambda_search = T)

## Simple GBM
data.gbm <- h2o.gbm(y = myY, x = myX, balance_classes = T, 
                    training_frame = airlines.hex, ntrees = 20, max_depth = 5,
                    distribution = "bernoulli", learn_rate = .1, min_rows = 2)

if(nrow(airlines.hex)==nrow(airlines_hdfs.hex)) stop("# rows are not equal!")
if(ncol(airlines.hex)==ncol(airlines_hdfs.hex)) stop("# columns not equal!")

PASS_BANNER()
