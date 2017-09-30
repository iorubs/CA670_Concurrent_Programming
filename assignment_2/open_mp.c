#include <omp.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
#include <unistd.h>

#define DATA_SIZE 2090152

int main() {

    int nums[DATA_SIZE];

    struct timeval start, end;

    FILE *myFile;
    myFile = fopen("input_nums", "r");

    if (myFile == NULL) {
        printf("Error Reading File\n");
        exit (1);
    }

    int i;
    for (i = 0; i < DATA_SIZE; i++) {
        fscanf(myFile, "%d,", &nums[i] );
    }

    fclose(myFile);

    int num_threads = omp_get_num_procs();
    int group_size = DATA_SIZE/num_threads;

    gettimeofday(&start , NULL);

    int sum=0;

    #pragma omp parallel num_threads(num_threads) shared(nums) reduction(+: sum)
    {
      int tid = omp_get_thread_num();
      int index = tid * group_size;
      int limit = (tid + 1) * group_size;

      if(tid == (num_threads-1)) {
        limit = DATA_SIZE;
      }

      while (index < limit) {
        sum += nums[index];
        index++;
      }
    }

    gettimeofday(&end , NULL);
    printf("sum reduction - %lu seconds and %lu microseconds \n",(end.tv_sec - start.tv_sec),(end.tv_usec - start.tv_usec));

    printf("sum = %d\n", sum);
}
