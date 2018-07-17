#! /usr/bin/Rscript

#
# Generate a graph.
# Total number of touches vs. auth_accuracy
# 	touches = base model touches + auth model touches
# 	auth_accuracy = correct authentications / total authentications
# (An authentication threshold will need to be chosen)
#

source("model_paramater.r")

pdf("output/total_vs_auth.pdf")

x<-c(file_data$total_model_size)
y<-c(1.0-fixed_FPR)-unlist(file_data$minimum_FNR,use.names=FALSE)

plot(x,y,
    xlab="Total Model Size", ylab="Authentication Accuracy", main="Total Interactions vs. Authentication Accuracy",
    xlim=c(0,1.0), ylim=c(0,1.0))

dev.off()
