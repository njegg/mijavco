# mijavco
 
> Work in progress compiler and virtual machine for a simple C/MicroJava like language

MicroJava is a simple language made by H. Mössenböck
for a Compiler Construction course whose materials were used in
Compiler Construction course at Faculty of Sciences, University of Novi Sad.
Language used in this project is a modified version of that language

### Example
For a file:
```java
program P
{
    int one() { return 1; }

	void main()
		int i;
	{
		i = 0;
		while (i < 5) {
			print(i);
			i = i + one();
		}
	}
}
```
it will generate bytecode and dump it in a file. It can also print the instructions
and some more info about generated file:

```
main: 16
size: 38

one:
008  enter           000 000          (0)
011  const_1         
012  exit            
013  return          
014  trap            001              (1)

main:
016  enter           000 001          (1)
019  const_0         
020  store_0         
021  load_0          
022  const_5         
023  jge             000 014          (14)
026  load_0          
027  print           
028  load_0          
029  call            000 008          (8)
032  add             
033  store_0         
034  jmp             -01 -13          (-13)
037  exit            
038  return          

Dump: 0 0 0 16 0 0 0 38 55 0 0 22 56 54 61 1 55 0 1 21 8 1 26 52 0 14 1 58 1 53 0 8 27 8 46 -1 -13 56 54 
```