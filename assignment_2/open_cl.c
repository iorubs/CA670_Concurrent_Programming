//
//  main.c
//  SumReductionOpenCL
//
//  Created by Ruben Vasconcelos on 4/5/17.
//

#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
#include <unistd.h>

#ifdef __APPLE__
#include <OpenCL/opencl.h>
#else
#include <CL/cl.h>
#endif

const char *ProgramSource =
"kernel void add(global int* input,\n"\
 "            const int group_size,\n"\
 "            const int data_size,\n"\
 "            global int* result) {\n"\
 "\n"\
 "    int index = get_global_id(0) * group_size;\n"\
 "    int sum = 0;\n"\
 "    int limit = (get_global_id(0) + 1) * group_size;\n"\
 "    if(get_global_id(0) == get_num_groups(0)-1){\n"\
 "        limit = data_size;\n"\
 "    }\n"\
 "\n"\
 "    while (index < limit) {\n"\
 "        sum += input[index];\n"\
 "        index++;\n"\
 "    }\n"\
 "    result[get_group_id(0)] = sum;\n"\
 "}\n";

int
main(void) {
    cl_context context;
    cl_context_properties properties[3];
    cl_kernel kernel;
    cl_command_queue command_queue;
    cl_program program;
    cl_int err;
    cl_uint num_of_platforms=0;
    cl_platform_id platform_id;
    cl_device_id device_id;
    cl_device_id all_device_id[2];
    cl_uint num_of_devices=0;
    cl_mem input, output;

    size_t numItems = 2000000;
    size_t itemsPerBlock = 250;

    int inputData[numItems];
    int result[numItems/itemsPerBlock];

    struct timeval start, end;

    FILE *myFile;
    myFile = fopen("input_nums", "r");

    //read file into array
    int i;

    if (myFile == NULL) {
        printf("Error Reading File\n");
        exit (0);
    }

    for (i = 0; i < numItems; i++) {
        fscanf(myFile, "%d,", &inputData[i] );
    }

    fclose(myFile);

    gettimeofday(&start , NULL);

    /*retreive a list of platforms available*/
    if(clGetPlatformIDs(1, &platform_id, &num_of_platforms) != CL_SUCCESS) {
        printf("Unable to get platform id\n");
        return 1;
    }

    /*try to get a supported GPU device*/
    if(clGetDeviceIDs(platform_id, CL_DEVICE_TYPE_GPU, 2, all_device_id, &num_of_devices) != CL_SUCCESS) {
        printf("Unable to get device id\n");
        return  1;
    }

    device_id = all_device_id[1];

    /*context properties list − must be terminated with 0*/
    properties[0] = CL_CONTEXT_PLATFORM;
    properties[1] = (cl_context_properties) platform_id;
    properties[2] = 0;

    /*create a context with the GPU device*/
    context = clCreateContext(properties, 1, &device_id, NULL, NULL, &err);

    /*create command queue using the context and device*/
    command_queue = clCreateCommandQueue(context, device_id, 0, &err);

    /*create a program from the kernel source code*/
    program=clCreateProgramWithSource(context, 1, (const char**) &ProgramSource, NULL, &err);

    /*compile the program*/
    if(clBuildProgram(program, 0, NULL, NULL, NULL, NULL) != CL_SUCCESS) {
        printf("Error building program\n");
        return 1;
    }

    /*specify which kernel from the program to execute*/
    kernel=clCreateKernel(program, "add", &err);

    /*create buffers for the input and ouput*/
    input = clCreateBuffer(context, CL_MEM_READ_WRITE, sizeof(int) * numItems, NULL, NULL);
    output = clCreateBuffer(context, CL_MEM_WRITE_ONLY, sizeof(int) * numItems, NULL, NULL);

    /*load data into the input buffer*/
    clEnqueueWriteBuffer(command_queue, input, CL_TRUE, 0, sizeof(int) * numItems, inputData, 0, NULL, NULL);

    /*set the argument list for the kernel command*/
    clSetKernelArg(kernel, 0, sizeof(cl_mem), (void *)&input);
    clSetKernelArg(kernel, 1, sizeof(int),&itemsPerBlock);
    clSetKernelArg(kernel, 2, sizeof(int),&numItems);
    clSetKernelArg(kernel, 3, sizeof(cl_mem), (void *)&output);

    /*enqueue the kernel command for execution*/
    gettimeofday(&end , NULL);
    printf("prep work - %lu seconds and %d microseconds \n",(end.tv_sec - start.tv_sec),(end.tv_usec - start.tv_usec));

    gettimeofday(&start , NULL);

    size_t commands = (numItems/itemsPerBlock);
    size_t oneWorkItem = 1;
    clEnqueueNDRangeKernel(command_queue, kernel, 1, NULL, &commands, &oneWorkItem, 0, NULL, NULL);

    clFinish(command_queue);

    /*copy the results from the output buffer*/
    clEnqueueReadBuffer(command_queue, output, CL_TRUE, 0, sizeof(int)* (numItems/itemsPerBlock), result, 0, NULL, NULL);

    int sum = result[0];

    for (i = 1; i < (numItems/itemsPerBlock); i++) {
        sum += result[i];
    }

    gettimeofday(&end , NULL);
    printf("sum reduction - %lu seconds and %d microseconds \n",(end.tv_sec - start.tv_sec),(end.tv_usec - start.tv_usec));

    printf("output: %d\n", sum);

    /*cleanup − release OpenCL resources*/
    clReleaseMemObject(input);
    clReleaseMemObject(output);
    clReleaseProgram(program);
    clReleaseKernel(kernel);
    clReleaseCommandQueue(command_queue);
    clReleaseContext(context);

    return(0);
}
