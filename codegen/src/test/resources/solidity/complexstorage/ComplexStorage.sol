//in order for experimental ABIEncoderV2 to be “stable” when passing structs as arguments we must
//use at least version 0.4.19 of solidity
pragma solidity ^0.6.5;
pragma experimental ABIEncoderV2;

contract ComplexStorage {

    struct Foo {
        string id;
        string name;
    }

    struct Bar {
        uint id;
        uint data;
    }

    struct Fuzz {
        Bar bar;
        uint data;
    }

    struct Baz {
        string id;
        uint data;
    }

    struct Boz {
        uint data;
        string id;
    }

    struct Nuu {
        Foo foo;
    }

    struct Nar {
        Nuu nuu;
    }

    struct Naz {
        Nar nar;
        uint data;
    }

    struct Wiz {
        Foo foo;
        string data;
    }

    Foo foo;
    Bar bar;
    Fuzz fuzz;
    Baz baz;
    Boz boz;
    Nuu nuu;
    Naz naz;
    Wiz wiz;

    constructor(Foo memory _foo, Bar memory _bar) public {
        foo = _foo;
        bar = _bar;
        emit Access(msg.sender, _foo, _bar);
    }

    function setFoo(Foo memory _toSet) public {
        foo = _toSet;
    }

    function setBar(Bar memory _toSet) public {
        bar = _toSet;
    }

    function setFuzz(Fuzz memory _toSet) public {
        fuzz = _toSet;
    }

    function setBaz(Baz memory _toSet) public {
        baz = _toSet;
    }

    function setBoz(Boz memory _toSet) public {
        boz = _toSet;
    }

    function setNuu(Nuu memory _toSet) public {
        nuu = _toSet;
    }

    function setNaz(Naz memory _naz) public {
        naz = _naz;
    }

    function setWiz(Wiz memory _toSet) public {
        wiz = _toSet;
    }

    function getFoo() public view returns (Foo memory) {
        return foo;
    }

    function getBar() public view returns (Bar memory) {
        return bar;
    }

    function getFuzz() public view returns (Fuzz memory) {
        return fuzz;
    }

    function getNaz() public view returns (Naz memory) {
        return naz;
    }

    function getWiz() public view returns (Wiz memory) {
        return wiz;
    }

    function getFooBar() public view returns (Foo memory, Bar memory) {
        return (foo, bar);
    }

    function getFooUint() public view returns (Foo memory, uint256) {
        return (foo, 1);
    }

    event Access(address indexed _address, Foo _foo, Bar _bar);

}